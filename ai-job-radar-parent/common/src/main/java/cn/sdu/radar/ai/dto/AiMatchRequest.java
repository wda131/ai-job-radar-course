package cn.sdu.radar.ai.dto;

import lombok.Data;

@Data
public class AiMatchRequest {
    private String userSkills;
    private String profileSummary;
    private String jobTitle;
    private String jobDescription;
    private String jobSkills;
    private Integer ruleScore;
}
