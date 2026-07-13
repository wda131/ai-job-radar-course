package cn.sdu.radar.pojo.vo;

import cn.sdu.radar.vo.JobSummaryVO;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApplicationVO {
    private Long id;
    private JobSummaryVO job;
    private String status;
    private String progressNote;
    private LocalDateTime appliedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
