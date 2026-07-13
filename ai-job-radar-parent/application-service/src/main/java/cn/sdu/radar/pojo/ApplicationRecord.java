package cn.sdu.radar.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("applications")
public class ApplicationRecord {
    private Long id;
    private Long userId;
    private Long jobId;
    private String status;
    private String progressNote;
    private LocalDateTime appliedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
