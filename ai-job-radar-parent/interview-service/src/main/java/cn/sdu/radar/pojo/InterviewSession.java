package cn.sdu.radar.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("interview_sessions")
public class InterviewSession {
    private Long id;
    private Long userId;
    private Long jobId;
    private String status;
    private Integer totalScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
