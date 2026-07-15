package cn.sdu.radar.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interview_answers")
public class InterviewAnswer {
    private Long id;
    private Long sessionId;
    private Long questionId;
    private String answer;
    private Integer score;
    private String feedback;
    private String strengths;
    private String weaknesses;
    private String suggestion;
    private Boolean aiUsed;
    private LocalDateTime createdAt;
}
