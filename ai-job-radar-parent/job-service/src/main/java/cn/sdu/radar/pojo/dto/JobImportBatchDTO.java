package cn.sdu.radar.pojo.dto;

import lombok.Data;

import java.util.List;

@Data
public class JobImportBatchDTO {
    private List<JobImportDTO> jobs;
}
