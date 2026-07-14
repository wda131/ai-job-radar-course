package cn.sdu.radar.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notifications")
public class Notification {
    private Long id;
    private String eventId;
    private Long userId;
    private String type;
    private String title;
    private String content;
    private Boolean readStatus;
    private LocalDateTime createdAt;
}
