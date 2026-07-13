package cn.sdu.radar.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("match_results")
public class MatchResult {
    private Long id;
    private Long userId;
    private Long jobId;
    private Integer score;
    private String matchedSkills;
    private String missingSkills;
    private String summary;
    private LocalDateTime createdAt;
}
