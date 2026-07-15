package cn.sdu.radar.controller;

import cn.sdu.radar.pojo.dto.JobImportBatchDTO;
import cn.sdu.radar.pojo.dto.JobImportDTO;
import cn.sdu.radar.pojo.vo.JobImportResultVO;
import cn.sdu.radar.service.JobImportService;
import cn.sdu.radar.service.JobService;
import cn.sdu.radar.utils.CommonResult;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobControllerTest {

    @Test
    void delegatesAuthenticatedImportBatchToImportService() {
        JobService jobService = mock(JobService.class);
        JobImportService importService = mock(JobImportService.class);
        JobController controller = new JobController(jobService, importService);
        JobImportDTO item = new JobImportDTO();
        item.setExternalId("abc123");
        JobImportBatchDTO batch = new JobImportBatchDTO();
        batch.setJobs(Collections.singletonList(item));
        JobImportResultVO expected = new JobImportResultVO();
        expected.setCreated(1);
        when(importService.importJobs(batch.getJobs())).thenReturn(expected);

        CommonResult<JobImportResultVO> response = controller.importJobs(batch);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getCreated());
        verify(importService).importJobs(batch.getJobs());
    }
}
