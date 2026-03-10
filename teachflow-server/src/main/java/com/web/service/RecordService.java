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
    // 用于模拟后台处理（避免阻塞接口线程）
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    @Autowired
    private XfyunConfig xfyunConfig;
    // 注入 OkHttpClient 或使用 RestTemplate
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

    // 上传建档（你之前跑通的那套：落盘 + 生成 videoUrl + 写库）
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
        // 你当前 Record 里已有这些字段的话就能直接赋值；没有的话请以你现有 Record 字段为准
        r.videoUrl = "/uploads/" + filename;
        r.durationSeconds = durationSeconds;
        r.failedReason = null;
        r.sceneType = (sceneType == null ? SceneType.GENERAL : sceneType);

        // sceneMetaJson：若非空则校验是否为合法 JSON，再保存
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

        // ✅ 只赋值一次 -> 变成有效 final（lambda 可用）
        final RecordStatus statusEnum =
                (status != null && !status.trim().isEmpty())
                        ? RecordStatus.valueOf(status.trim())   // Controller 已校验过
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

        // ✅ 先判断存在再删，避免 deleteById 在不存在时抛异常
        if (recordTranscriptRepository.existsById(id)) {
            recordTranscriptRepository.deleteById(id);
        }
        if (recordAnalysisRepository.existsById(id)) {
            recordAnalysisRepository.deleteById(id);
        }

        // 尝试删磁盘文件（删不掉也不影响删除记录）
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
    // ✅ process：改状态为 PROCESSING，并异步生成“占位结果”，最后置为 COMPLETED/FAILED
    public ProcessView process(Long id) {
        Record r = recordRepository.findById(id).orElse(null);
        if (r == null) return null;

        // ✅ 对齐文档：PROCESSING/COMPLETED 不允许再次开始处理 -> Controller 会映射成 2002
        if (r.status == RecordStatus.PROCESSING || r.status == RecordStatus.COMPLETED) {
            throw new IllegalStateException("NOT_OPERABLE");
        }

        // ✅ 重试前清理旧结果（避免 FAILED 重试时还返回旧转写/旧分析）
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

    // 后台收尾：写两张表 + 改 record 状态
    private void finishProcess(Long id) {
        try {

            Record r = recordRepository.findById(id).orElse(null);
            if (r == null) return; // 处理中被删了

            // 声明两个变量，稍后赋值
            String signatureRandom = null;
            String orderId = null;

            // 从 videoUrl 中提取文件名
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

// 获取文件大小（字节）
            long fileSize = audioFile.length();

            // 确保 durationSeconds 有效
            if (r.durationSeconds == null || r.durationSeconds <= 0) {
                // 使用已有的 File 对象（假设你叫 audioFile）
                long parsedMs = MediaUtil.getDurationMs(audioFile.getAbsolutePath());
                if (parsedMs <= 0) {
                    throw new RuntimeException("无法从媒体文件中解析时长");
                }
                r.durationSeconds = (int) (parsedMs / 1000);
                recordRepository.save(r);
                log.info("已从文件解析时长并更新记录: {} 秒", r.durationSeconds);
            }
// 现在 durationSeconds 肯定有值
            long durationMs = r.durationSeconds * 1000L;

            // 1. 构建参数 Map（注意：值使用原始值，不要编码）
            Map<String, String> params = new HashMap<>();
            params.put("appId", xfyunConfig.getAppId());
            params.put("accessKeyId", xfyunConfig.getApiKey());
            params.put("dateTime", XfyunSignUtil.getCurrentDateTime());
            signatureRandom = XfyunSignUtil.generateRandom16(); // 保存起来，后面轮询可能要用
            params.put("signatureRandom", signatureRandom);
            params.put("fileSize", String.valueOf(fileSize));
            params.put("fileName", fileName); // 例如 "abc123.mp4"
            params.put("duration", String.valueOf(durationMs));
            params.put("language", "autodialect");

// 2. 生成签名
            String signature = XfyunSignUtil.generateSignature(params, xfyunConfig.getApiSecret());

            // 使用 StringBuilder 拼接完整的上传地址
            StringBuilder urlBuilder = new StringBuilder(xfyunConfig.getUploadUrl()).append("?");
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
                urlBuilder.append(entry.getKey()).append("=").append(encodedValue).append("&");
            }
// 删除最后一个多余的 '&'
            urlBuilder.deleteCharAt(urlBuilder.length() - 1);
            String uploadUrl = urlBuilder.toString();

// 可选：打印日志方便调试
            System.out.println("上传URL: " + uploadUrl);

            // 创建文件请求体
            RequestBody fileBody = RequestBody.create(audioFile, MediaType.parse("application/octet-stream"));

// 构建 HTTP 请求
            Request request = new Request.Builder()
                    .url(uploadUrl)
                    .addHeader("Content-Type", "application/octet-stream")
                    .addHeader("signature", signature)  // 之前生成的签名
                    .post(fileBody)
                    .build();

// 发送请求并处理响应
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "无响应体";
                    throw new RuntimeException("上传失败，HTTP code: " + response.code() + ", body: " + errorBody);
                }

                String responseBody = response.body().string();
                System.out.println("上传响应: " + responseBody); // 调试日志

                // 解析 JSON 响应（使用 Jackson 的 ObjectMapper）
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(responseBody);
                String code = root.get("code").asText();

                if (!"000000".equals(code)) {
                    String desc = root.get("descInfo").asText();
                    throw new RuntimeException("上传失败: " + desc);
                }

                // 提取 orderId
                orderId = root.get("content").get("orderId").asText();
                System.out.println("获取到 orderId: " + orderId);

                // 这里先保存 orderId 到 Record（如果需要），然后进行第五步轮询
                // 为了测试，我们暂时只打印 orderId，后续再添加轮询
                // 你可以选择将 orderId 存入 Record 的一个字段，或者直接用局部变量继续轮询
                // 我们将在下一步添加轮询代码，所以这里先保留 orderId 在局部变量中
                // 注意：如果后续轮询需要用到 signatureRandom，它已经在局部变量中

                // 接下来将进入轮询查询结果的步骤（第五步）
                // 我们会在后面补充轮询代码

            } catch (Exception e) {
                // 上传失败，更新记录状态为 FAILED
                r.status = RecordStatus.FAILED;
                r.failedReason = "上传异常: " + e.getMessage();
                recordRepository.save(r);
                return; // 结束当前处理
            }

            // 轮询获取转写结果（返回原始 orderResult 字符串）
            String orderResult = pollForResult(orderId, signatureRandom);
            if (orderResult == null) {
                throw new RuntimeException("转写超时或失败");
            }

// 解析成 TranscriptView 对象
            TranscriptView transcriptView = parseXfyunResult(orderResult);

// 将 TranscriptView 序列化为 JSON 字符串（只包含 segments 字段）
            ObjectMapper mapper = new ObjectMapper(); // 可以使用类成员变量
            String transcriptJson = mapper.writeValueAsString(transcriptView);

// 保存转写结果
            LocalDateTime now = LocalDateTime.now();
            recordTranscriptRepository.save(new RecordTranscript(id, transcriptJson, now));

// 保存分析结果
            // 假设你已经有了 transcriptView 和 fullText
            String fullText = transcriptView.segments.stream()
                    .map(s -> s.text)
                    .collect(Collectors.joining(" "));

// 调用 AI 分析
            String sceneTypeStr = r.sceneType != null ? r.sceneType.name() : "GENERAL";
            SiliconFlowUtil.AnalysisResult aiResult = siliconFlowUtil.analyze(fullText, sceneTypeStr);



// 构造 outlineList（因为 AnalysisView 的 outline 是 List<Object>）
            List<Object> outlineList = null;
            if (aiResult.outline != null) {
                outlineList = new ArrayList<>();
                outlineList.add(aiResult.outline);
            }

// 创建 AnalysisView，传入 outlineList
            AnalysisView analysisView = new AnalysisView(
                    aiResult.summary,
                    aiResult.keywords,
                    outlineList
            );

// 序列化并保存
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

    /**
     * 轮询查询转写结果
     * @param orderId 订单ID
     * @param signatureRandom 上传时使用的随机串（复用）
     * @return 转写结果的 JSON 字符串（content 部分），如果超时或失败返回 null
     */
    private String pollForResult(String orderId, String signatureRandom) {
        int maxAttempts = 30;          // 最多轮询30次
        int interval = 3000;            // 每次间隔3秒
        int attempt = 0;

        while (attempt < maxAttempts) {
            try {
                Thread.sleep(interval);

                // 构建查询参数
                Map<String, String> queryParams = new HashMap<>();
                queryParams.put("accessKeyId", xfyunConfig.getApiKey());
                queryParams.put("dateTime", XfyunSignUtil.getCurrentDateTime());
                queryParams.put("signatureRandom", signatureRandom);
                queryParams.put("orderId", orderId);
                queryParams.put("resultType", "transfer");

                // 生成查询签名
                String querySignature = XfyunSignUtil.generateSignature(queryParams, xfyunConfig.getApiSecret());

                // 构造查询URL
                StringBuilder urlBuilder = new StringBuilder(xfyunConfig.getGetResultUrl()).append("?");
                for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                    String encodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
                    urlBuilder.append(entry.getKey()).append("=").append(encodedValue).append("&");
                }
                urlBuilder.deleteCharAt(urlBuilder.length() - 1);
                String queryUrl = urlBuilder.toString();

                // 创建请求体（空的JSON对象）
                RequestBody body = RequestBody.create("{}".getBytes(StandardCharsets.UTF_8), MediaType.parse("application/json"));

                Request request = new Request.Builder()
                        .url(queryUrl)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("signature", querySignature)
                        .post(body)
                        .build();

                try (Response response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        // 请求失败，重试
                        attempt++;
                        continue;
                    }

                    String responseBody = response.body().string();
                    System.out.println("查询响应: " + responseBody); // 调试

                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(responseBody);
                    String code = root.get("code").asText();

                    if (!"000000".equals(code)) {
                        // 可能还在处理中，继续轮询
                        attempt++;
                        continue;
                    }

                    JsonNode content = root.get("content");
// 检查 content 中是否有 orderResult 字段，且不为空字符串
                    if (content != null && content.has("orderResult") && !content.get("orderResult").asText().isEmpty()) {
                        // 可以返回整个 content 或只返回 orderResult 内容，根据你的需求
                        // 注意：orderResult 本身是一个 JSON 字符串，可能需要再解析
                        return content.get("orderResult").asText();

                    } else {
                        // 没有结果，继续轮询
                        attempt++;
                    }
                } catch (Exception e) {
                    // 网络或解析异常，继续轮询
                    attempt++;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null; // 超时
    }

    private TranscriptView parseXfyunResult(String orderResult) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(orderResult);
        System.out.println("开始解析 orderResult，总长度：" + orderResult.length());

        // 优先使用 lattice2（最终结果），如果没有则用 lattice
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

            // 获取时间戳
            if (!node.has("begin") || !node.has("end")) {
                System.out.println("跳过：缺少 begin 或 end");
                continue;
            }
            long startMs = node.get("begin").asLong();
            long endMs = node.get("end").asLong();
            System.out.println("时间范围：" + startMs + " -> " + endMs);

            // 获取 json_1best
            if (!node.has("json_1best")) {
                System.out.println("跳过：缺少 json_1best");
                continue;
            }
            JsonNode json1bestNode = node.get("json_1best");
            JsonNode stNode;
            if (useLattice2) {
                // lattice2 中 json_1best 已经是对象
                stNode = json1bestNode;
            } else {
                // lattice 中 json_1best 是字符串，需要解析
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
        // 1) 先确认 record 存在（否则就是 2001）
        Record r = recordRepository.findById(id).orElse(null);
        if (r == null) return null;

        // 2) 去分表查 transcript
        com.web.model.RecordTranscript rt = recordTranscriptRepository.findById(id).orElse(null);
        if (rt == null || rt.transcriptJson == null || rt.transcriptJson.isBlank()) {
            // 用 segments=null 表达“未就绪”，让 Controller 返回 3001
            return new com.web.dto.TranscriptView(null);
        }

        // 3) 把 JSON 字符串反序列化成对象返回
        return objectMapper.readValue(rt.transcriptJson, com.web.dto.TranscriptView.class);
    }

    public com.web.dto.AnalysisView getAnalysis(Long id) throws Exception {
        // 1) 先确认 record 存在
        Record r = recordRepository.findById(id).orElse(null);
        if (r == null) return null;

        // 2) 去分表查 analysis
        com.web.model.RecordAnalysis ra = recordAnalysisRepository.findById(id).orElse(null);
        if (ra == null || ra.analysisJson == null || ra.analysisJson.isBlank()) {
            // 用 summary=null 表达“未就绪”，让 Controller 返回 3002
            return new com.web.dto.AnalysisView(null, null, null);
        }

        // 3) JSON -> 对象
        return objectMapper.readValue(ra.analysisJson, com.web.dto.AnalysisView.class);
    }
}