package cn.sdu.radar.vo;

import lombok.Data;

@Data
public class UserProfileVO {
    private Long id;
    private String username;
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
