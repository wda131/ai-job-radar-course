package cn.sdu.radar.ai.dto;

import lombok.Data;

@Data
public class AiQuestionRequest {
    private Long jobId;
    private String jobTitle;
    private String jobDescription;
    private String jobSkills;
}
