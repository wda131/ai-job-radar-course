package cn.sdu.radar.ai.dto;

import lombok.Data;

@Data
public class AiEvaluationRequest {
    private String jobTitle;
    private String jobSkills;
    private String question;
    private String answer;
}
