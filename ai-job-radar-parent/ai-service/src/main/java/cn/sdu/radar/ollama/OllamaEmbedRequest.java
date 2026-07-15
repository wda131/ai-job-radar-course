package cn.sdu.radar.ollama;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OllamaEmbedRequest {
    private String model;
    private String input;
}
