package cn.sdu.radar.pojo;

import cn.sdu.radar.pojo.dto.JobImportBatchDTO;
import cn.sdu.radar.pojo.dto.JobImportDTO;
import cn.sdu.radar.pojo.vo.JobImportResultVO;
import cn.sdu.radar.vo.JobSummaryVO;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobImportContractsTest {

    @Test
    void exposesImportedJobFieldsAcrossRequestAndResponseContracts() {
        JobImportDTO item = new JobImportDTO();
        item.setSource("BOSS");
        item.setExternalId("abc123");
        item.setTitle("Java 开发工程师");
        item.setCompany("海纳科技");
        item.setCity("威海");
        item.setSalaryMin(10000);
        item.setSalaryMax(16000);
        item.setExperienceYears(1);
        item.setEducation("本科");
        item.setDescription("负责后端服务");
        item.setRequirements("Java Spring Boot");
        item.setWelfareTags("双休,五险一金");
        item.setSourceUrl("https://www.zhipin.com/job_detail/abc123.html");
        item.setStatus("OPEN");

        JobImportBatchDTO batch = new JobImportBatchDTO();
        batch.setJobs(Collections.singletonList(item));
        assertEquals("abc123", batch.getJobs().get(0).getExternalId());
        assertEquals(10000, batch.getJobs().get(0).getSalaryMin());

        JobImportResultVO result = new JobImportResultVO();
        result.setCreated(1);
        result.setUpdated(2);
        result.setRejected(3);
        result.setErrors(Collections.singletonList("第 4 条数据无效"));
        assertEquals(6, result.getCreated() + result.getUpdated() + result.getRejected());
        assertEquals(1, result.getErrors().size());

        JobSummaryVO summary = new JobSummaryVO();
        summary.setSource("BOSS");
        summary.setSourceUrl(item.getSourceUrl());
        assertEquals("BOSS", summary.getSource());
        assertEquals(item.getSourceUrl(), summary.getSourceUrl());
    }
}
