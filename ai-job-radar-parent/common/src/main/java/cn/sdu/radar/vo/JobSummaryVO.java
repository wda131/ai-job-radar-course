package cn.sdu.radar.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JobSummaryVO {
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
