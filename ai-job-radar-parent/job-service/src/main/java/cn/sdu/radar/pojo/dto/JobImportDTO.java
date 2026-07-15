package cn.sdu.radar.pojo.dto;

import lombok.Data;

@Data
public class JobImportDTO {
    private String source;
    private String externalId;
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
    private String sourceUrl;
    private String status;
}
