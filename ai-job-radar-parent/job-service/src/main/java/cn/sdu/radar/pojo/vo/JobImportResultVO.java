package cn.sdu.radar.pojo.vo;

import lombok.Data;

import java.util.List;

@Data
public class JobImportResultVO {
    private Integer created;
    private Integer updated;
    private Integer rejected;
    private List<String> errors;
}
