package com.web.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class SiliconFlowUtil {

    private static final Logger log = LoggerFactory.getLogger(SiliconFlowUtil.class);
    private static final String API_URL = "https://api.siliconflow.cn/v1/chat/completions";
    private final OkHttpClient client;
    private final ObjectMapper mapper;
    private final String apiKey;

    // 从 application.yml 中读取 siliconflow.api-key
    public SiliconFlowUtil(@Value("${siliconflow.api-key}") String apiKey) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.mapper = new ObjectMapper();
        this.apiKey = apiKey;
        if (this.apiKey == null || this.apiKey.isEmpty()) {
            log.error("siliconflow.api-key 未在 application.yml 中配置！");
        } else {
            log.info("SiliconFlowUtil 初始化成功，API Key 已加载");
        }
    }

    public AnalysisResult analyze(String text, String sceneType) throws IOException {
        if (text == null || text.trim().isEmpty()) {
            return new AnalysisResult(new ArrayList<>(), "", null);
        }

        String truncated = text.length() > 2000 ? text.substring(0, 2000) : text;

        // 根据场景类型构造不同的 outline 指令
        String outlineInstruction = "";
        if ("HOMEWORK_CHECK".equals(sceneType)) {
            outlineInstruction =
                    "3. 按作业检查场景生成结构化大纲（outline），格式如下：\n" +
                            "   {\n" +
                            "     \"type\": \"HOMEWORK_CHECK_REPORT\",\n" +
                            "     \"header\": {\n" +
                            "       \"conclusion\": \"一句话结论\",\n" +
                            "       \"tags\": [{\"name\": \"标签名\", \"count\": 1}]\n" +
                            "     },\n" +
                            "     \"panels\": [\n" +
                            "       {\n" +
                            "         \"panelId\": \"p1\",\n" +
                            "         \"questionNo\": \"题号\",\n" +
                            "         \"title\": \"标题\",\n" +
                            "         \"items\": [\n" +
                            "           {\n" +
                            "             \"issueType\": \"问题类型\",\n" +
                            "             \"issue\": \"问题描述\",\n" +
                            "             \"suggestion\": \"改法\",\n" +
                            "             \"evidence\": {\"startMs\": 0, \"endMs\": 0}\n" +
                            "           }\n" +
                            "         ]\n" +
                            "       }\n" +
                            "     ],\n" +
                            "     \"todo\": [\n" +
                            "       {\n" +
                            "         \"todoId\": \"t1\",\n" +
                            "         \"title\": \"待办标题\",\n" +
                            "         \"detail\": \"详细要求\",\n" +
                            "         \"relatedQuestionNo\": \"题号\",\n" +
                            "         \"evidence\": {\"startMs\": 0, \"endMs\": 0},\n" +
                            "         \"status\": \"TODO\"\n" +
                            "       }\n" +
                            "     ]\n" +
                            "   }";
        } else if ("DEFENSE".equals(sceneType)) {
            outlineInstruction =
                    "3. 按答辩场景生成结构化大纲（outline），格式如下：\n" +
                            "   {\n" +
                            "     \"type\": \"DEFENSE_REPORT\",\n" +
                            "     \"header\": {\n" +
                            "       \"verdict\": \"结论\",\n" +
                            "       \"overallConclusion\": \"总体结论\",\n" +
                            "       \"tags\": [{\"name\": \"标签名\", \"count\": 1}]\n" +
                            "     },\n" +
                            "     \"panels\": [\n" +
                            "       {\n" +
                            "         \"panelId\": \"p1\",\n" +
                            "         \"category\": \"维度\",\n" +
                            "         \"title\": \"标题\",\n" +
                            "         \"items\": [\n" +
                            "           {\n" +
                            "             \"issueType\": \"问题类型\",\n" +
                            "             \"issue\": \"问题描述\",\n" +
                            "             \"suggestion\": \"修改建议\",\n" +
                            "             \"priority\": \"P0\",\n" +
                            "             \"evidence\": {\"startMs\": 0, \"endMs\": 0}\n" +
                            "           }\n" +
                            "         ]\n" +
                            "       }\n" +
                            "     ],\n" +
                            "     \"todo\": [\n" +
                            "       {\n" +
                            "         \"todoId\": \"t1\",\n" +
                            "         \"title\": \"待办标题\",\n" +
                            "         \"detail\": \"详细要求\",\n" +
                            "         \"priority\": \"P0\",\n" +
                            "         \"evidence\": {\"startMs\": 0, \"endMs\": 0},\n" +
                            "         \"status\": \"TODO\"\n" +
                            "       }\n" +
                            "     ]\n" +
                            "   }";
        } else {
            outlineInstruction =
                    "3. 按通用讲课场景生成结构化大纲（outline），格式如下：\n" +
                            "   {\n" +
                            "     \"type\": \"GENERAL_REPORT\",\n" +
                            "     \"topic\": \"课程主题（从文本中提取）\",\n" +
                            "     \"sections\": [\n" +
                            "       {\n" +
                            "         \"title\": \"第一部分标题\",\n" +
                            "         \"keyPoints\": [\"要点1\", \"要点2\"]\n" +
                            "       },\n" +
                            "       {\n" +
                            "         \"title\": \"第二部分标题\",\n" +
                            "         \"keyPoints\": [\"要点1\", \"要点2\"]\n" +
                            "       }\n" +
                            "     ]\n" +
                            "   }";
        }

        String prompt = String.format(
                "请从以下文本中提取信息，并按 JSON 格式返回，包含三个字段：keywords（字符串数组）、summary（字符串）、outline（对象）。\n\n" +
                        "文本：%s\n\n" +
                        "要求：\n" +
                        "1. keywords：提取5-10个关键词。\n" +
                        "2. summary：生成1-2句话摘要。\n" +
                        "%s\n\n" +
                        "请确保返回的 JSON 格式正确，不要包含多余的解释。",
                truncated, outlineInstruction
        );

        ObjectNode requestBody = mapper.createObjectNode();
        requestBody.put("model", "deepseek-ai/DeepSeek-V2.5");

        ArrayNode messages = mapper.createArrayNode();
        ObjectNode userMessage = mapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);
        requestBody.set("messages", messages);
        requestBody.put("temperature", 0.3);
        requestBody.put("max_tokens", 500);

        Request request = new Request.Builder()
                .url(API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(
                        mapper.writeValueAsString(requestBody),
                        MediaType.parse("application/json")
                ))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "";
                throw new IOException("API 调用失败，code: " + response.code() + ", body: " + errorBody);
            }

            String respBody = response.body().string();
            JsonNode root = mapper.readTree(respBody);

            String content = root.path("choices").get(0)
                    .path("message").path("content").asText();

            // 清理可能存在的 markdown 代码块标记
            content = content.trim();
            if (content.startsWith("```json")) {
                content = content.substring(7); // 去掉开头的 ```json
                if (content.endsWith("```")) {
                    content = content.substring(0, content.length() - 3);
                }
            } else if (content.startsWith("```")) {
                content = content.substring(3);
                if (content.endsWith("```")) {
                    content = content.substring(0, content.length() - 3);
                }
            }
            content = content.trim();

            return mapper.readValue(content, AnalysisResult.class);
        }
    }

    public static class AnalysisResult {
        public List<String> keywords;
        public String summary;
        public Object outline;  // 新增，因为 outline 可以是任意 JSON 结构

        public AnalysisResult() {}

        public AnalysisResult(List<String> keywords, String summary, Object outline) {
            this.keywords = keywords;
            this.summary = summary;
            this.outline = outline;
        }
    }
}