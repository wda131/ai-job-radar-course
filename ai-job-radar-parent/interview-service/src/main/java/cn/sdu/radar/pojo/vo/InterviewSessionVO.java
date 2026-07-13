package cn.sdu.radar.pojo.vo;

import cn.sdu.radar.vo.JobSummaryVO;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class InterviewSessionVO {
    private Long id;
    private String status;
    private Integer totalScore;
    private JobSummaryVO job;
    private List<InterviewQuestionVO> questions;
    private LocalDateTime createdAt;
}
