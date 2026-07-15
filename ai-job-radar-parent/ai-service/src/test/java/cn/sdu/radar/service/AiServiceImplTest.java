package cn.sdu.radar.service;

import cn.sdu.radar.ai.dto.AiEmbeddingRequest;
import cn.sdu.radar.ai.dto.AiEvaluationRequest;
import cn.sdu.radar.ai.dto.AiMatchRequest;
import cn.sdu.radar.ai.dto.AiQuestionRequest;
import cn.sdu.radar.config.AiProperties;
import cn.sdu.radar.ollama.OllamaClient;
import cn.sdu.radar.service.impl.AiServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiServiceImplTest {
    private AiProperties properties;
    private OllamaClient ollamaClient;
    private AiServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new AiProperties();
        properties.setEnabled(true);
        ollamaClient = mock(OllamaClient.class);
        service = new AiServiceImpl(properties, ollamaClient, new ObjectMapper());
    }

    @Test
    void disabledAiReturnsUnavailableWithoutCallingOllama() {
        properties.setEnabled(false);

        assertFalse(service.match(new AiMatchRequest()).isAvailable());
        verify(ollamaClient, never()).embed(anyString());
    }

    @Test
    void embeddingReturnsCosineSimilarityAsPercentage() {
        when(ollamaClient.embed("Java Spring" )).thenReturn(new double[]{1.0, 0.0});
        when(ollamaClient.embed("Java backend")).thenReturn(new double[]{0.8, 0.6});
        AiEmbeddingRequest request = new AiEmbeddingRequest();
        request.setSourceText("Java Spring");
        request.setTargetText("Java backend");

        assertTrue(service.embedding(request).isAvailable());
        assertEquals(80, service.embedding(request).getSimilarityScore());
    }

    @Test
    void matchParsesStructuredJsonAndClampsSemanticScore() {
        when(ollamaClient.embed(anyString())).thenReturn(new double[]{1.0, 0.0});
        when(ollamaClient.chat(anyString(), anyString())).thenReturn("{\"summary\":\"总体匹配\",\"strengths\":[\"Java\"],\"gaps\":[\"Redis\"],\"suggestions\":[\"补充项目\"]}");

        assertTrue(service.match(matchRequest()).isAvailable());
        assertEquals(100, service.match(matchRequest()).getSemanticScore());
        assertEquals("总体匹配", service.match(matchRequest()).getSummary());
    }

    @Test
    void invalidChatJsonReturnsUnavailable() {
        when(ollamaClient.embed(anyString())).thenReturn(new double[]{1.0, 0.0});
        when(ollamaClient.chat(anyString(), anyString())).thenReturn("not-json");

        assertFalse(service.match(matchRequest()).isAvailable());
    }

    @Test
    void ollamaExceptionReturnsUnavailable() {
        when(ollamaClient.embed(anyString())).thenThrow(new IllegalStateException("offline"));

        assertFalse(service.match(matchRequest()).isAvailable());
    }

    @Test
    void exactlyFourQuestionsAreAccepted() {
        when(ollamaClient.chat(anyString(), anyString())).thenReturn("{\"questions\":[\"Q1\",\"Q2\",\"Q3\",\"Q4\"]}");

        assertTrue(service.questions(new AiQuestionRequest()).isAvailable());
        assertEquals(4, service.questions(new AiQuestionRequest()).getQuestions().size());
    }

    @Test
    void wrongQuestionCountReturnsUnavailable() {
        when(ollamaClient.chat(anyString(), anyString())).thenReturn("{\"questions\":[\"Q1\",\"Q2\",\"Q3\"]}");

        assertFalse(service.questions(new AiQuestionRequest()).isAvailable());
    }

    @Test
    void evaluationScoreIsClampedToOneHundred() {
        when(ollamaClient.chat(anyString(), anyString())).thenReturn("{\"score\":120,\"strengths\":[\"表达清楚\"],\"weaknesses\":[\"细节不足\"],\"suggestion\":\"补充数据\"}");

        assertTrue(service.evaluate(new AiEvaluationRequest()).isAvailable());
        assertEquals(100, service.evaluate(new AiEvaluationRequest()).getScore());
    }

    @Test
    void evaluationPromptRequiresHundredPointScale() {
        when(ollamaClient.chat(anyString(), anyString())).thenReturn("{\"score\":80,\"strengths\":[\"清楚\"],\"weaknesses\":[\"细节\"],\"suggestion\":\"补充\"}");

        service.evaluate(new AiEvaluationRequest());

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(ollamaClient).chat(anyString(), prompt.capture());
        assertTrue(prompt.getValue().contains("0到100"));
        assertTrue(prompt.getValue().contains("禁止使用十分制"));
    }

    private AiMatchRequest matchRequest() {
        AiMatchRequest request = new AiMatchRequest();
        request.setUserSkills("Java,Spring Boot");
        request.setProfileSummary("后端项目经验");
        request.setJobTitle("Java开发工程师");
        request.setJobDescription("负责后端服务开发");
        request.setJobSkills("Java,Spring Boot,Redis");
        request.setRuleScore(80);
        return request;
    }
}
