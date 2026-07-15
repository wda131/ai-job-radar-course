package cn.sdu.radar.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class InterviewAnswerVO {
    private Long id;
    private Long questionId;
    private String answer;
    private Integer score;
    private String feedback;
    private List<String> strengths;
    private List<String> weaknesses;
    private String suggestion;
    private boolean aiUsed;
    private LocalDateTime createdAt;
}
