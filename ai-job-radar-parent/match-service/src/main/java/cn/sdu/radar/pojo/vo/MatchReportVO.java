package cn.sdu.radar.pojo.vo;

import cn.sdu.radar.vo.JobSummaryVO;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MatchReportVO {
    private Long id;
    private Integer score;
    private String matchedSkills;
    private String missingSkills;
    private String summary;
    private JobSummaryVO job;
    private LocalDateTime createdAt;
}
