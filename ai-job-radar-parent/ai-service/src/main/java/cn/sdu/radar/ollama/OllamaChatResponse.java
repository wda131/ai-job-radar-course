package cn.sdu.radar.ollama;

import lombok.Data;

@Data
public class OllamaChatResponse {
    private Message message;

    @Data
    public static class Message {
        private String role;
        private String content;
    }
}
