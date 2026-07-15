package cn.sdu.radar.ai.dto;

import lombok.Data;

@Data
public class AiEmbeddingRequest {
    private String sourceText;
    private String targetText;
}
