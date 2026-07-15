package cn.sdu.radar.ai.dto;

import lombok.Data;

@Data
public class AiEmbeddingResponse {
    private boolean available;
    private Integer similarityScore;
}
