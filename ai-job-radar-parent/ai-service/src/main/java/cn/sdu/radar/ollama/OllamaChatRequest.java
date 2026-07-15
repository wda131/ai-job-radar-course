package cn.sdu.radar.ollama;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
public class OllamaChatRequest {
    private String model;
    private List<Message> messages;
    private boolean stream = false;
    private boolean think = false;
    private String format = "json";
    private Map<String, Object> options = Collections.<String, Object>singletonMap("temperature", 0.2);

    public OllamaChatRequest(String model, String systemPrompt, String userPrompt) {
        this.model = model;
        this.messages = Arrays.asList(
                new Message("system", systemPrompt),
                new Message("user", userPrompt)
        );
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }
}
