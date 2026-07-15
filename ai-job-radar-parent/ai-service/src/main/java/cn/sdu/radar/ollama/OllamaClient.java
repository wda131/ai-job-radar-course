package cn.sdu.radar.ollama;

import cn.sdu.radar.config.AiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

@Component
public class OllamaClient {
    private final AiProperties properties;
    private final RestTemplate restTemplate;

    @Autowired
    public OllamaClient(AiProperties properties, RestTemplateBuilder builder) {
        this.properties = properties;
        Duration timeout = Duration.ofSeconds(properties.getTimeoutSeconds());
        this.restTemplate = builder
                .setConnectTimeout(timeout)
                .setReadTimeout(timeout)
                .build();
    }

    public boolean health() {
        restTemplate.getForObject(url("/api/tags"), String.class);
        return true;
    }

    public String chat(String systemPrompt, String userPrompt) {
        OllamaChatResponse response = restTemplate.postForObject(
                url("/api/chat"),
                new OllamaChatRequest(properties.getChatModel(), systemPrompt, userPrompt),
                OllamaChatResponse.class
        );
        if (response == null || response.getMessage() == null || response.getMessage().getContent() == null) {
            throw new IllegalStateException("Ollama未返回对话内容");
        }
        return response.getMessage().getContent();
    }

    public double[] embed(String text) {
        OllamaEmbedResponse response = restTemplate.postForObject(
                url("/api/embed"),
                new OllamaEmbedRequest(properties.getEmbeddingModel(), text),
                OllamaEmbedResponse.class
        );
        if (response == null || response.getEmbeddings() == null || response.getEmbeddings().isEmpty()) {
            throw new IllegalStateException("Ollama未返回向量");
        }
        List<Double> values = response.getEmbeddings().get(0);
        double[] vector = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            vector[i] = values.get(i);
        }
        return vector;
    }

    private String url(String path) {
        String baseUrl = properties.getOllamaBaseUrl();
        return (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl) + path;
    }
}
