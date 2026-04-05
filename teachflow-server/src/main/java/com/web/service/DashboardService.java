package com.web.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.web.model.Record;
import com.web.model.RecordAnalysis;
import com.web.model.RecordStatus;
import com.web.repository.RecordAnalysisRepository;
import com.web.repository.RecordRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final RecordRepository recordRepository;
    private final RecordAnalysisRepository analysisRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 依赖注入：把底层的两个仓库管理员请过来
    public DashboardService(RecordRepository recordRepository, RecordAnalysisRepository analysisRepository) {
        this.recordRepository = recordRepository;
        this.analysisRepository = analysisRepository;
    }

    // 核心加工方法：返回一个装满各类统计数据的 Map 给前端
    public Map<String, Object> getDashboardData() {
        Map<String, Object> result = new HashMap<>();

        // ================= 1. 顶部数据卡片 =================
        long totalRecords = recordRepository.count(); // 标配方法
        Long sumSeconds = recordRepository.sumDurationSeconds(); // 我们自己写的特殊方法 1
        long totalDuration = (sumSeconds != null) ? sumSeconds / 60 : 0; // 转换成分钟
        long completedRecords = recordRepository.countByStatus(RecordStatus.COMPLETED); // 我们自己写的特殊方法 2

        // 计算成功率 (保留一位小数)
        double successRate = totalRecords == 0 ? 0 : Math.round((completedRecords * 1000.0) / totalRecords) / 10.0;

        result.put("totalRecords", totalRecords);
        result.put("totalDuration", totalDuration);
        result.put("successRate", successRate);

        // ================= 2. 饼图：场景分布 =================
        List<Record> allRecords = recordRepository.findAll();
        Map<String, Long> sceneCount = allRecords.stream()
                .filter(r -> r.sceneType != null)
                .collect(Collectors.groupingBy(r -> r.sceneType.name(), Collectors.counting()));

        List<Map<String, Object>> pieData = new ArrayList<>();
        sceneCount.forEach((type, count) -> {
            Map<String, Object> item = new HashMap<>();
            String name = type.equals("HOMEWORK_CHECK") ? "作业检查" : (type.equals("DEFENSE") ? "答辩环节" : "通用教学");
            item.put("name", name);
            item.put("value", count);
            pieData.add(item);
        });
        result.put("pieData", pieData);

        // ================= 3. 折线图：近7天活跃趋势 =================
        LocalDate today = LocalDate.now();
        Map<String, Long> trendMap = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");

        // 铺垫一个空的7天日历，保证某天没数据时数量为0，不然前端画出来的折线图会断层
        for (int i = 6; i >= 0; i--) {
            trendMap.put(today.minusDays(i).format(fmt), 0L);
        }

        LocalDateTime weekAgo = today.minusDays(6).atStartOfDay();
        // 我们自己写的特殊方法 3：查最近7天的数据
        List<Record> recentRecords = recordRepository.findByCreatedAtAfter(weekAgo);
        for (Record r : recentRecords) {
            if(r.createdAt != null) {
                String dateStr = r.createdAt.toLocalDate().format(fmt);
                if(trendMap.containsKey(dateStr)) {
                    trendMap.put(dateStr, trendMap.get(dateStr) + 1);
                }
            }
        }
        result.put("trendDates", new ArrayList<>(trendMap.keySet()));
        result.put("trendCounts", new ArrayList<>(trendMap.values()));

        // ================= 4. 条形图：高频关键词提取 =================
        List<RecordAnalysis> analyses = analysisRepository.findAll();
        Map<String, Integer> wordFreq = new HashMap<>();

        // 遍历所有 AI 分析的 JSON，把里面的 keywords 掏出来算词频
        for (RecordAnalysis a : analyses) {
            try {
                if (a.analysisJson != null && !a.analysisJson.isBlank()) {
                    JsonNode root = objectMapper.readTree(a.analysisJson);
                    JsonNode keywords = root.get("keywords");
                    if (keywords != null && keywords.isArray()) {
                        for (JsonNode kw : keywords) {
                            String word = kw.asText();
                            wordFreq.put(word, wordFreq.getOrDefault(word, 0) + 1);
                        }
                    }
                }
            } catch (Exception ignored) {
                // 如果某条 JSON 坏了，直接忽略，不影响大局
            }
        }

        // 按频率倒序，只挑出前 10 名
        List<Map.Entry<String, Integer>> topWords = wordFreq.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(10)
                .toList();

        // ECharts 条形图要求数据是从下往上画的，所以这里反着塞进列表
        List<String> words = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (int i = topWords.size() - 1; i >= 0; i--) {
            words.add(topWords.get(i).getKey());
            counts.add(topWords.get(i).getValue());
        }
        result.put("barWords", words);
        result.put("barCounts", counts);

        return result;
    }
}