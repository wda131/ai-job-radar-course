package cn.sdu.radar.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiMatchResponse {
    private boolean available;
    private Integer semanticScore;
    private String summary;
    private List<String> strengths;
    private List<String> gaps;
    private List<String> suggestions;
}
