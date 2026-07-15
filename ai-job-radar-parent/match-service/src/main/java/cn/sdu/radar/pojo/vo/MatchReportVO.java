package cn.sdu.radar.pojo.vo;

import cn.sdu.radar.vo.JobSummaryVO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MatchReportVO {
    private Long id;
    private Integer score;
    private Integer ruleScore;
    private Integer semanticScore;
    private boolean aiUsed;
    private String matchedSkills;
    private String missingSkills;
    private String summary;
    private List<String> strengths;
    private List<String> gaps;
    private List<String> suggestions;
    private JobSummaryVO job;
    private LocalDateTime createdAt;
}
