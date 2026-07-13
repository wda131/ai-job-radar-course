package cn.sdu.radar.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("favorites")
public class Favorite {
    private Long id;
    private Long userId;
    private Long jobId;
    private LocalDateTime createdAt;
}
