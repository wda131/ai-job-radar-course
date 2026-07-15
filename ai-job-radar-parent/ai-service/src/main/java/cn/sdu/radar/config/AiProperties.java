package cn.sdu.radar.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    private boolean enabled = true;
    private String ollamaBaseUrl = "http://127.0.0.1:11434";
    private String chatModel = "qwen3:8b";
    private String embeddingModel = "qwen3-embedding:4b";
    private int timeoutSeconds = 15;
}
