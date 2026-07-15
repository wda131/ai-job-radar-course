package cn.sdu.radar.service.impl;

import cn.sdu.radar.ai.dto.AiEmbeddingRequest;
import cn.sdu.radar.ai.dto.AiEmbeddingResponse;
import cn.sdu.radar.ai.dto.AiEvaluationRequest;
import cn.sdu.radar.ai.dto.AiEvaluationResponse;
import cn.sdu.radar.ai.dto.AiMatchRequest;
import cn.sdu.radar.ai.dto.AiMatchResponse;
import cn.sdu.radar.ai.dto.AiQuestionRequest;
import cn.sdu.radar.ai.dto.AiQuestionResponse;
import cn.sdu.radar.config.AiProperties;
import cn.sdu.radar.ollama.OllamaClient;
import cn.sdu.radar.service.AiService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiServiceImpl implements AiService {
    private static final String JSON_SYSTEM_PROMPT = "你是求职辅助系统，只输出合法JSON，不要Markdown，不要解释。";

    private final AiProperties properties;
    private final OllamaClient ollamaClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public AiServiceImpl(AiProperties properties, OllamaClient ollamaClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.ollamaClient = ollamaClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean health() {
        if (!properties.isEnabled()) {
            return false;
        }
        try {
            return ollamaClient.health();
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public AiEmbeddingResponse embedding(AiEmbeddingRequest request) {
        AiEmbeddingResponse response = new AiEmbeddingResponse();
        if (!properties.isEnabled() || request == null || blank(request.getSourceText()) || blank(request.getTargetText())) {
            return response;
        }
        try {
            response.setSimilarityScore(cosineScore(
                    ollamaClient.embed(request.getSourceText()),
                    ollamaClient.embed(request.getTargetText())
            ));
            response.setAvailable(true);
        } catch (RuntimeException exception) {
            response.setAvailable(false);
        }
        return response;
    }

    @Override
    public AiMatchResponse match(AiMatchRequest request) {
        AiMatchResponse response = new AiMatchResponse();
        if (!properties.isEnabled() || request == null) {
            return response;
        }
        try {
            String source = join(request.getUserSkills(), request.getProfileSummary());
            String target = join(request.getJobTitle(), request.getJobDescription(), request.getJobSkills());
            int semanticScore = cosineScore(ollamaClient.embed(source), ollamaClient.embed(target));
            String prompt = "分析候选人与岗位的匹配。候选人：" + source + "\n岗位：" + target
                    + "\n规则分：" + request.getRuleScore()
                    + "\n输出字段：summary字符串、strengths字符串数组、gaps字符串数组、suggestions字符串数组。";
            JsonNode json = objectMapper.readTree(ollamaClient.chat(JSON_SYSTEM_PROMPT, prompt));
            response.setSemanticScore(clamp(semanticScore));
            response.setSummary(requiredText(json, "summary"));
            response.setStrengths(requiredStringList(json, "strengths"));
            response.setGaps(requiredStringList(json, "gaps"));
            response.setSuggestions(requiredStringList(json, "suggestions"));
            response.setAvailable(true);
        } catch (Exception exception) {
            response.setAvailable(false);
        }
        return response;
    }

    @Override
    public AiQuestionResponse questions(AiQuestionRequest request) {
        AiQuestionResponse response = new AiQuestionResponse();
        if (!properties.isEnabled() || request == null) {
            return response;
        }
        try {
            String prompt = "为岗位生成4道由浅入深、可直接回答的模拟面试题。岗位："
                    + join(request.getJobTitle(), request.getJobDescription(), request.getJobSkills())
                    + "\n只输出questions字符串数组。";
            JsonNode json = objectMapper.readTree(ollamaClient.chat(JSON_SYSTEM_PROMPT, prompt));
            List<String> questions = requiredStringList(json, "questions");
            if (questions.size() != 4) {
                return response;
            }
            response.setQuestions(questions);
            response.setAvailable(true);
        } catch (Exception exception) {
            response.setAvailable(false);
        }
        return response;
    }

    @Override
    public AiEvaluationResponse evaluate(AiEvaluationRequest request) {
        AiEvaluationResponse response = new AiEvaluationResponse();
        if (!properties.isEnabled() || request == null) {
            return response;
        }
        try {
            String prompt = "评价模拟面试回答。岗位：" + join(request.getJobTitle(), request.getJobSkills())
                    + "\n问题：" + request.getQuestion() + "\n回答：" + request.getAnswer()
                    + "\n输出字段：score必须是0到100之间整数，禁止使用十分制；"
                    + "strengths字符串数组、weaknesses字符串数组、suggestion字符串。";
            JsonNode json = objectMapper.readTree(ollamaClient.chat(JSON_SYSTEM_PROMPT, prompt));
            JsonNode score = json.get("score");
            if (score == null || !score.isNumber()) {
                return response;
            }
            response.setScore(clamp(score.asInt()));
            response.setStrengths(requiredStringList(json, "strengths"));
            response.setWeaknesses(requiredStringList(json, "weaknesses"));
            response.setSuggestion(requiredText(json, "suggestion"));
            response.setAvailable(true);
        } catch (Exception exception) {
            response.setAvailable(false);
        }
        return response;
    }

    private int cosineScore(double[] left, double[] right) {
        if (left == null || right == null || left.length == 0 || left.length != right.length) {
            throw new IllegalArgumentException("向量维度不一致");
        }
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            throw new IllegalArgumentException("向量不能为零");
        }
        return clamp((int) Math.round(dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm)) * 100));
    }

    private List<String> requiredStringList(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isArray()) {
            throw new IllegalArgumentException("缺少字段：" + field);
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (!item.isTextual() || blank(item.asText())) {
                throw new IllegalArgumentException("字段格式错误：" + field);
            }
            values.add(item.asText().trim());
        }
        return values;
    }

    private String requiredText(JsonNode json, String field) {
        JsonNode node = json.get(field);
        if (node == null || !node.isTextual() || blank(node.asText())) {
            throw new IllegalArgumentException("缺少字段：" + field);
        }
        return node.asText().trim();
    }

    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private String join(String... values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (!blank(value)) {
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(value.trim());
            }
        }
        return builder.toString();
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
