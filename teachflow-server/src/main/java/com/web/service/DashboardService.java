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


    public DashboardService(RecordRepository recordRepository, RecordAnalysisRepository analysisRepository) {
        this.recordRepository = recordRepository;
        this.analysisRepository = analysisRepository;
    }


    public Map<String, Object> getDashboardData() {
        Map<String, Object> result = new HashMap<>();


        long totalRecords = recordRepository.count();
        Long sumSeconds = recordRepository.sumDurationSeconds();
        long totalDuration = (sumSeconds != null) ? sumSeconds / 60 : 0;
        long completedRecords = recordRepository.countByStatus(RecordStatus.COMPLETED);


        double successRate = totalRecords == 0 ? 0 : Math.round((completedRecords * 1000.0) / totalRecords) / 10.0;

        result.put("totalRecords", totalRecords);
        result.put("totalDuration", totalDuration);
        result.put("successRate", successRate);


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


        LocalDate today = LocalDate.now();
        Map<String, Long> trendMap = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");


        for (int i = 6; i >= 0; i--) {
            trendMap.put(today.minusDays(i).format(fmt), 0L);
        }

        LocalDateTime weekAgo = today.minusDays(6).atStartOfDay();

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


        List<RecordAnalysis> analyses = analysisRepository.findAll();
        Map<String, Integer> wordFreq = new HashMap<>();


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

            }
        }


        List<Map.Entry<String, Integer>> topWords = wordFreq.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(10)
                .toList();


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