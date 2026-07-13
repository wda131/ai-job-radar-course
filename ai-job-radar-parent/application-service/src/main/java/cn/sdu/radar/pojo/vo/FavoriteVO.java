package cn.sdu.radar.pojo.vo;

import cn.sdu.radar.vo.JobSummaryVO;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FavoriteVO {
    private Long id;
    private JobSummaryVO job;
    private LocalDateTime createdAt;
}
