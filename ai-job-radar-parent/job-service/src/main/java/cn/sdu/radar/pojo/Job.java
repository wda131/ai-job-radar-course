package cn.sdu.radar.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("jobs")
public class Job {
    private Long id;
    private String title;
    private String company;
    private String city;
    private Integer salaryMin;
    private Integer salaryMax;
    private Integer experienceYears;
    private String education;
    private String description;
    private String requirements;
    private String welfareTags;
    private String status;
    private LocalDateTime postedAt;
}
