package com.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.config.XfyunConfig;
import com.web.dto.*;
import com.web.model.Record;
import com.web.model.RecordAnalysis;
import com.web.model.RecordStatus;
import com.web.model.RecordTranscript;
import com.web.model.SceneType;
import com.web.repository.RecordAnalysisRepository;
import com.web.repository.RecordRepository;
import com.web.repository.RecordTranscriptRepository;
import com.web.util.MediaUtil;
import com.web.util.SiliconFlowUtil;
import com.web.util.XfyunSignUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

@Slf4j
@Service
public class RecordService {

    private final RecordRepository recordRepository;
    private final RecordTranscriptRepository recordTranscriptRepository;
    private final RecordAnalysisRepository recordAnalysisRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    @Autowired
    private XfyunConfig xfyunConfig;

    private final OkHttpClient httpClient = new OkHttpClient();

    @Autowired
    private SiliconFlowUtil siliconFlowUtil;

    public RecordService(RecordRepository recordRepository,
                         RecordTranscriptRepository recordTranscriptRepository,
                         RecordAnalysisRepository recordAnalysisRepository) {
        this.recordRepository = recordRepository;
        this.recordTranscriptRepository = recordTranscriptRepository;
        this.recordAnalysisRepository = recordAnalysisRepository;
    }


    public Record create(MultipartFile file, String title, Integer durationSeconds,
                         SceneType sceneType, String sceneMetaJson) throws IOException {
        Path uploadDir = Paths.get("uploads");
        Files.createDirectories(uploadDir);

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null) {
            int dot = original.lastIndexOf('.');
            if (dot >= 0) ext = original.substring(dot);
        }
        String filename = UUID.randomUUID().toString().replace("-", "") + ext;

        Path target = uploadDir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        String finalTitle = (title != null && !title.trim().isEmpty())
                ? title.trim()
                : (original != null && !original.trim().isEmpty() ? original.trim() : "未命名");

        Record r = new Record(null, finalTitle, RecordStatus.UPLOADED, LocalDateTime.now());

        r.videoUrl = "/uploads/" + filename;
        r.durationSeconds = durationSeconds;
        r.failedReason = null;
        r.sceneType = (sceneType == null ? SceneType.GENERAL : sceneType);


        if (sceneMetaJson != null && !sceneMetaJson.trim().isEmpty()) {
            try {
                objectMapper.readTree(sceneMetaJson);
            } catch (Exception e) {
                throw new IllegalArgumentException("invalid sceneMeta (must be JSON)");
            }
            r.sceneMetaJson = sceneMetaJson.trim();
        } else {
            r.sceneMetaJson = null;
        }

        return recordRepository.save(r);
    }


    public ListView<RecordItemView> list(int page, int size) {
        Page<Record> p = recordRepository.findAll(
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        List<RecordItemView> items = p.getContent().stream()
                .map(r -> new RecordItemView(
                        r.id,
                        r.title,
                        r.durationSeconds,
                        r.status == null ? null : r.status.name(),
                        r.createdAt == null ? null : r.createdAt.format(DT_FMT),
                        (r.sceneType == null ? SceneType.GENERAL : r.sceneType).name()
                ))
                .toList();

        return new ListView<>(items, p.getTotalElements());
    }

    public ListView<RecordItemView> list(int page, int size,
                                         String keyword, String status,
                                         LocalDateTime dateFrom, LocalDateTime dateTo,
                                         SceneType sceneType) {


        final RecordStatus statusEnum =
                (status != null && !status.trim().isEmpty())
                        ? RecordStatus.valueOf(status.trim())
                        : null;

        final SceneType sceneTypeEnum = sceneType;

        Specification<Record> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = "%" + keyword.trim().toLowerCase() + "%";
                ps.add(cb.like(cb.lower(root.get("title")), kw));
            }
            if (statusEnum != null) {
                ps.add(cb.equal(root.get("status"), statusEnum));
            }
            if (sceneTypeEnum != null) {
                ps.add(cb.equal(root.get("sceneType"), sceneTypeEnum));
            }
            if (dateFrom != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom));
            }
            if (dateTo != null) {
                ps.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo));
            }

            return cb.and(ps.toArray(new Predicate[0]));
        };

        Page<Record> p = recordRepository.findAll(
                spec,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        List<RecordItemView> items = p.getContent().stream()
                .map(r -> new RecordItemView(
                        r.id,
                        r.title,
                        r.durationSeconds,
                        r.status == null ? null : r.status.name(),
                        r.createdAt == null ? null : r.createdAt.format(DT_FMT),
                        (r.sceneType == null ? SceneType.GENERAL : r.sceneType).name()
                ))
                .toList();

        return new ListView<>(items, p.getTotalElements());
    }

    public Record get(Long id) {
        return recordRepository.findById(id).orElse(null);
    }

    public boolean delete(Long id) {
        Record r = recordRepository.findById(id).orElse(null);
        if (r == null) return false;


        if (recordTranscriptRepository.existsById(id)) {
            recordTranscriptRepository.deleteById(id);
        }
        if (recordAnalysisRepository.existsById(id)) {
            recordAnalysisRepository.deleteById(id);
        }


        try {
            if (r.videoUrl != null && r.videoUrl.startsWith("/uploads/")) {
                String filename = r.videoUrl.substring("/uploads/".length());
                Path filePath = Paths.get("uploads").resolve(filename);
                Files.deleteIfExists(filePath);
            }
        } catch (Exception ignored) {}

        recordRepository.deleteById(id);
        return true;
    }

    public ProcessView process(Long id) {
        Record r = recordRepository.findById(id).orElse(null);
        if (r == null) return null;


        if (r.status == RecordStatus.PROCESSING || r.status == RecordStatus.COMPLETED) {
            throw new IllegalStateException("NOT_OPERABLE");
        }


        if (recordTranscriptRepository.existsById(id)) {
            recordTranscriptRepository.deleteById(id);
        }
        if (recordAnalysisRepository.existsById(id)) {
            recordAnalysisRepository.deleteById(id);
        }

        r.status = RecordStatus.PROCESSING;
        r.failedReason = null;
        recordRepository.save(r);

        CompletableFuture.runAsync(() -> finishProcess(id), executor);

        return new ProcessView(r.id, r.status.name());
    }


    private void finishProcess(Long id) {
        try {

            Record r = recordRepository.findById(id).orElse(null);
            if (r == null) return;


            String signatureRandom = null;
            String orderId = null;


            String videoUrl = r.videoUrl;
            if (videoUrl == null || !videoUrl.startsWith("/uploads/")) {
                throw new RuntimeException("无效的视频路径");
            }
            String fileName = videoUrl.substring("/uploads/".length());
            Path filePath = Paths.get("uploads").resolve(fileName);
            File audioFile = filePath.toFile();

            if (!audioFile.exists()) {
                throw new RuntimeException("音频文件不存在：" + filePath);
            }


            long fileSize = audioFile.length();


            if (r.durationSeconds == null || r.durationSeconds <= 0) {

                long parsedMs = MediaUtil.getDurationMs(audioFile.getAbsolutePath());
                if (parsedMs <= 0) {
                    throw new RuntimeException("无法从媒体文件中解析时长");
                }
                r.durationSeconds = (int) (parsedMs / 1000);
                recordRepository.save(r);
                log.info("已从文件解析时长并更新记录: {} 秒", r.durationSeconds);
            }

            long durationMs = r.durationSeconds * 1000L;


            Map<String, String> params = new HashMap<>();
            params.put("appId", xfyunConfig.getAppId());
            params.put("accessKeyId", xfyunConfig.getApiKey());
            params.put("dateTime", XfyunSignUtil.getCurrentDateTime());
            signatureRandom = XfyunSignUtil.generateRandom16();
            params.put("signatureRandom", signatureRandom);
            params.put("fileSize", String.valueOf(fileSize));
            params.put("fileName", fileName);
            params.put("duration", String.valueOf(durationMs));
            params.put("language", "autodialect");


            String signature = XfyunSignUtil.generateSignature(params, xfyunConfig.getApiSecret());


            StringBuilder urlBuilder = new StringBuilder(xfyunConfig.getUploadUrl()).append("?");
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
                urlBuilder.append(entry.getKey()).append("=").append(encodedValue).append("&");
            }

            urlBuilder.deleteCharAt(urlBuilder.length() - 1);
            String uploadUrl = urlBuilder.toString();


            System.out.println("上传URL: " + uploadUrl);


            RequestBody fileBody = RequestBody.create(audioFile, MediaType.parse("application/octet-stream"));


            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .addHeader("Content-Type", "application/octet-stream")
                    .addHeader("signature", signature)
                    .post(fileBody)
                    .build();


            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "无响应体";
                    throw new RuntimeException("上传失败，HTTP code: " + response.code() + ", body: " + errorBody);
                }

                String responseBody = response.body().string();
                System.out.println("上传响应: " + responseBody);


                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(responseBody);
                String code = root.get("code").asText();

                if (!"000000".equals(code)) {
                    String desc = root.get("descInfo").asText();
                    throw new RuntimeException("上传失败: " + desc);
                }


                orderId = root.get("content").get("orderId").asText();
                System.out.println("获取到 orderId: " + orderId);










            } catch (Exception e) {

                r.status = RecordStatus.FAILED;
                r.failedReason = "上传异常: " + e.getMessage();
                recordRepository.save(r);
                return;
            }


            String orderResult = pollForResult(orderId, signatureRandom);
            if (orderResult == null) {
                throw new RuntimeException("转写超时或失败");
            }



            if (!recordRepository.existsById(id)) {
                log.warn("检测到记录 [{}] 已被用户删除，终止后续保存流程", id);
                return;
            }




            TranscriptView transcriptView = parseXfyunResult(orderResult);


            ObjectMapper mapper = new ObjectMapper();
            String transcriptJson = mapper.writeValueAsString(transcriptView);


            LocalDateTime now = LocalDateTime.now();
            recordTranscriptRepository.save(new RecordTranscript(id, transcriptJson, now));



            String fullText = transcriptView.segments.stream()
                    .map(s -> s.text)
                    .collect(Collectors.joining(" "));


            String sceneTypeStr = r.sceneType != null ? r.sceneType.name() : "GENERAL";
            String sceneMeta = (r.sceneMetaJson != null && !r.sceneMetaJson.isBlank()) ? r.sceneMetaJson : "无附加背景信息";
            SiliconFlowUtil.AnalysisResult aiResult = siliconFlowUtil.analyze(fullText, sceneTypeStr, sceneMeta);




            List<Object> outlineList = null;
            if (aiResult.outline != null) {
                outlineList = new ArrayList<>();
                outlineList.add(aiResult.outline);
            }


            AnalysisView analysisView = new AnalysisView(
                    aiResult.summary,
                    aiResult.keywords,
                    outlineList
            );


            String analysisJson = objectMapper.writeValueAsString(analysisView);
            recordAnalysisRepository.save(new RecordAnalysis(id, analysisJson, LocalDateTime.now()));

            r.status = RecordStatus.COMPLETED;
            r.failedReason = null;
            recordRepository.save(r);


        } catch (Exception e) {
            Record r = recordRepository.findById(id).orElse(null);
            if (r == null) return;
            r.status = RecordStatus.FAILED;
            r.failedReason = "process failed: " + e.getMessage();
            recordRepository.save(r);
        }
    }


    private String pollForResult(String orderId, String signatureRandom) {
        int maxAttempts = 30;
        int interval = 3000;
        int attempt = 0;

        while (attempt < maxAttempts) {
            try {
                Thread.sleep(interval);


                Map<String, String> queryParams = new HashMap<>();
                queryParams.put("accessKeyId", xfyunConfig.getApiKey());
                queryParams.put("dateTime", XfyunSignUtil.getCurrentDateTime());
                queryParams.put("signatureRandom", signatureRandom);
                queryParams.put("orderId", orderId);
                queryParams.put("resultType", "transfer");


                String querySignature = XfyunSignUtil.generateSignature(queryParams, xfyunConfig.getApiSecret());


                StringBuilder urlBuilder = new StringBuilder(xfyunConfig.getGetResultUrl()).append("?");
                for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                    String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
                    urlBuilder.append(entry.getKey()).append("=").append(encodedValue).append("&");
                }
                urlBuilder.deleteCharAt(urlBuilder.length() - 1);
                String queryUrl = urlBuilder.toString();


                RequestBody body = RequestBody.create("{}".getBytes(StandardCharsets.UTF_8), MediaType.parse("application/json"));

                Request request = new Request.Builder()
                        .url(queryUrl)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("signature", querySignature)
                        .post(body)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {

                        attempt++;
                        continue;
                    }

                    String responseBody = response.body().string();
                    System.out.println("查询响应: " + responseBody);

                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(responseBody);
                    String code = root.get("code").asText();

                    if (!"000000".equals(code)) {

                        attempt++;
                        continue;
                    }

                    JsonNode content = root.get("content");

                    if (content != null && content.has("orderResult") && !content.get("orderResult").asText().isEmpty()) {


                        return content.get("orderResult").asText();

                    } else {

                        attempt++;
                    }
                } catch (Exception e) {

                    attempt++;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    private TranscriptView parseXfyunResult(String orderResult) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(orderResult);
        System.out.println("开始解析 orderResult，总长度：" + orderResult.length());


        JsonNode lattice = root.get("lattice2");
        boolean useLattice2 = (lattice != null && lattice.isArray());
        if (!useLattice2) {
            lattice = root.get("lattice");
            if (lattice == null || !lattice.isArray()) {
                throw new RuntimeException("无法解析转写结果：缺少 lattice 或 lattice2 字段");
            }
        }
        System.out.println("lattice 格子数量：" + lattice.size());

        List<Segment> segments = new ArrayList<>();
        for (int i = 0; i < lattice.size(); i++) {
            JsonNode node = lattice.get(i);
            System.out.println("\n--- 处理第 " + i + " 个格子 ---");


            if (!node.has("begin") || !node.has("end")) {
                System.out.println("跳过：缺少 begin 或 end");
                continue;
            }
            long startMs = node.get("begin").asLong();
            long endMs = node.get("end").asLong();
            System.out.println("时间范围：" + startMs + " -> " + endMs);


            if (!node.has("json_1best")) {
                System.out.println("跳过：缺少 json_1best");
                continue;
            }
            JsonNode json1bestNode = node.get("json_1best");
            JsonNode stNode;
            if (useLattice2) {

                stNode = json1bestNode;
            } else {

                String json1best = json1bestNode.asText();
                if (json1best == null || json1best.isEmpty()) {
                    System.out.println("跳过：json_1best 为空");
                    continue;
                }
                System.out.println("json_1best 前100字符：" + json1best.substring(0, Math.min(100, json1best.length())));
                stNode = mapper.readTree(json1best);
            }

            JsonNode rtNode = stNode.path("st").path("rt");
            if (!rtNode.isArray() || rtNode.size() == 0) {
                System.out.println("跳过：rt 无效");
                continue;
            }

            JsonNode wsNode = rtNode.get(0).path("ws");
            if (!wsNode.isArray()) {
                System.out.println("跳过：ws 无效");
                continue;
            }

            StringBuilder textBuilder = new StringBuilder();
            for (JsonNode ws : wsNode) {
                JsonNode cwArray = ws.get("cw");
                if (cwArray != null && cwArray.isArray()) {
                    for (JsonNode cw : cwArray) {
                        JsonNode wordNode = cw.get("w");
                        if (wordNode != null) {
                            textBuilder.append(wordNode.asText());
                        }
                    }
                }
            }
            String text = textBuilder.toString().trim();
            System.out.println("提取的文本：" + (text.isEmpty() ? "[空]" : text));

            if (!text.isEmpty()) {
                segments.add(new Segment(startMs, endMs, text, null));
                System.out.println("已添加 segment");
            } else {
                System.out.println("文本为空，跳过");
            }
        }

        System.out.println("\n解析完成，共生成 " + segments.size() + " 个 segment");
        return new TranscriptView(segments);
    }
    public com.web.dto.TranscriptView getTranscript(Long id) throws Exception {

        Record r = recordRepository.findById(id).orElse(null);
        if (r == null) return null;


        com.web.model.RecordTranscript rt = recordTranscriptRepository.findById(id).orElse(null);
        if (rt == null || rt.transcriptJson == null || rt.transcriptJson.isBlank()) {

            return new com.web.dto.TranscriptView(null);
        }


        return objectMapper.readValue(rt.transcriptJson, com.web.dto.TranscriptView.class);
    }

    public com.web.dto.AnalysisView getAnalysis(Long id) throws Exception {

        Record r = recordRepository.findById(id).orElse(null);
        if (r == null) return null;


        com.web.model.RecordAnalysis ra = recordAnalysisRepository.findById(id).orElse(null);
        if (ra == null || ra.analysisJson == null || ra.analysisJson.isBlank()) {

            return new com.web.dto.AnalysisView(null, null, null);
        }


        return objectMapper.readValue(ra.analysisJson, com.web.dto.AnalysisView.class);
    }
}