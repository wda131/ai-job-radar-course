package cn.sdu.radar.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {
    private Long id;
    private String username;
    private String password;
    private String name;
    private String targetRole;
    private String city;
    private String skills;
    private Integer experienceYears;
    private String education;
    private Integer salaryMin;
    private Integer salaryMax;
    private String introduction;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
