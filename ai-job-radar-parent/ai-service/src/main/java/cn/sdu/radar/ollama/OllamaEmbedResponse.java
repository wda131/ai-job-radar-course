package cn.sdu.radar.ollama;

import lombok.Data;

import java.util.List;

@Data
public class OllamaEmbedResponse {
    private List<List<Double>> embeddings;
}
