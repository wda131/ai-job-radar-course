package cn.sdu.radar.service;

import cn.sdu.radar.exception.BusinessException;
import cn.sdu.radar.feign.JobClient;
import cn.sdu.radar.mapper.ApplicationRecordMapper;
import cn.sdu.radar.pojo.ApplicationRecord;
import cn.sdu.radar.pojo.dto.ApplicationCreateDTO;
import cn.sdu.radar.pojo.dto.ApplicationUpdateDTO;
import cn.sdu.radar.pojo.vo.ApplicationVO;
import cn.sdu.radar.service.impl.ApplicationServiceImpl;
import cn.sdu.radar.utils.CommonResult;
import cn.sdu.radar.vo.JobSummaryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplicationServiceImplTest {
    private ApplicationRecordMapper applicationMapper;
    private JobClient jobClient;
    private ApplicationServiceImpl applicationService;

    @BeforeEach
    void setUp() {
        applicationMapper = mock(ApplicationRecordMapper.class);
        jobClient = mock(JobClient.class);
        applicationService = new ApplicationServiceImpl(applicationMapper, jobClient);
        JobSummaryVO job = new JobSummaryVO();
        job.setId(4L);
        job.setTitle("全栈开发实习生");
        when(jobClient.getJob(4L)).thenReturn(CommonResult.success(job));
    }

    @Test
    void createsAppliedApplication() {
        when(applicationMapper.selectCount(any())).thenReturn(0);
        ApplicationCreateDTO input = new ApplicationCreateDTO();
        input.setJobId(4L);
        input.setProgressNote("准备完善简历");

        ApplicationVO result = applicationService.create(1L, input);

        assertEquals("APPLIED", result.getStatus());
        assertNotNull(result.getAppliedAt());
        assertEquals("全栈开发实习生", result.getJob().getTitle());
        verify(applicationMapper).insert(any(ApplicationRecord.class));
    }

    @Test
    void updatesToAppliedAndRecordsTime() {
        ApplicationRecord record = new ApplicationRecord();
        record.setId(8L);
        record.setUserId(1L);
        record.setJobId(4L);
        record.setStatus("PREPARING");
        when(applicationMapper.selectById(8L)).thenReturn(record);
        ApplicationUpdateDTO input = new ApplicationUpdateDTO();
        input.setStatus("APPLIED");
        input.setProgressNote("已提交简历");

        ApplicationVO result = applicationService.update(1L, 8L, input);

        assertEquals("APPLIED", result.getStatus());
        assertNotNull(record.getAppliedAt());
        verify(applicationMapper).updateById(record);
    }

    @Test
    void rejectsUnknownStatus() {
        ApplicationRecord record = new ApplicationRecord();
        record.setId(8L);
        record.setUserId(1L);
        record.setJobId(4L);
        when(applicationMapper.selectById(8L)).thenReturn(record);
        ApplicationUpdateDTO input = new ApplicationUpdateDTO();
        input.setStatus("UNKNOWN");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> applicationService.update(1L, 8L, input));

        assertEquals(400, exception.getCode());
    }
}
