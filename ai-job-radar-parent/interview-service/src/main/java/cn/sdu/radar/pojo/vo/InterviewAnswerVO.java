package cn.sdu.radar.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InterviewAnswerVO {
    private Long id;
    private Long questionId;
    private String answer;
    private Integer score;
    private String feedback;
    private LocalDateTime createdAt;
}
