package cn.sdu.radar.pojo.dto;

import lombok.Data;

@Data
public class ProfileUpdateDTO {
    private String name;
    private String targetRole;
    private String city;
    private String skills;
    private Integer experienceYears;
    private String education;
    private Integer salaryMin;
    private Integer salaryMax;
    private String introduction;
}
