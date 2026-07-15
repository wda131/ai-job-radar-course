package cn.sdu.radar.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiEvaluationResponse {
    private boolean available;
    private Integer score;
    private List<String> strengths;
    private List<String> weaknesses;
    private String suggestion;
}
