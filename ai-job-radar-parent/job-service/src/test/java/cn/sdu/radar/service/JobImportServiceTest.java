package cn.sdu.radar.service;

import cn.sdu.radar.exception.BusinessException;
import cn.sdu.radar.mapper.JobMapper;
import cn.sdu.radar.pojo.Job;
import cn.sdu.radar.pojo.dto.JobImportDTO;
import cn.sdu.radar.pojo.vo.JobImportResultVO;
import cn.sdu.radar.search.JobSearchRepository;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobImportServiceTest {

    private JobMapper jobMapper;
    private JobSearchRepository jobSearchRepository;
    private JobImportService service;

    @BeforeEach
    void setUp() {
        jobMapper = mock(JobMapper.class);
        jobSearchRepository = mock(JobSearchRepository.class);
        service = new JobImportService(jobMapper, jobSearchRepository);
    }

    @Test
    void insertsJobWhenExternalIdentityDoesNotExist() {
        when(jobMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        when(jobMapper.insert(any(Job.class))).thenAnswer(invocation -> {
            invocation.<Job>getArgument(0).setId(101L);
            return 1;
        });

        JobImportResultVO result = service.importJobs(Collections.singletonList(validJob("abc123")));

        assertEquals(1, result.getCreated());
        assertEquals(0, result.getUpdated());
        assertEquals(0, result.getRejected());
        ArgumentCaptor<Job> inserted = ArgumentCaptor.forClass(Job.class);
        verify(jobMapper).insert(inserted.capture());
        assertEquals("BOSS", inserted.getValue().getSource());
        assertEquals("abc123", inserted.getValue().getExternalId());
        assertEquals("10-16K", inserted.getValue().getSalaryText());
        assertNotNull(inserted.getValue().getImportedAt());
        verify(jobSearchRepository).saveAll(anyList());
    }

    @Test
    void updatesJobWhenExternalIdentityAlreadyExists() {
        Job existing = new Job();
        existing.setId(55L);
        existing.setSource("BOSS");
        existing.setExternalId("abc123");
        when(jobMapper.selectOne(any(QueryWrapper.class))).thenReturn(existing);

        JobImportResultVO result = service.importJobs(Collections.singletonList(validJob("abc123")));

        assertEquals(0, result.getCreated());
        assertEquals(1, result.getUpdated());
        ArgumentCaptor<Job> updated = ArgumentCaptor.forClass(Job.class);
        verify(jobMapper).updateById(updated.capture());
        assertEquals(55L, updated.getValue().getId());
        assertEquals("Java 开发工程师", updated.getValue().getTitle());
    }

    @Test
    void rejectsInvalidRowWhileImportingValidRow() {
        JobImportDTO invalid = validJob("bad-row");
        invalid.setTitle(" ");
        when(jobMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        when(jobMapper.insert(any(Job.class))).thenAnswer(invocation -> {
            invocation.<Job>getArgument(0).setId(102L);
            return 1;
        });

        JobImportResultVO result = service.importJobs(Arrays.asList(invalid, validJob("good-row")));

        assertEquals(1, result.getCreated());
        assertEquals(1, result.getRejected());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("第 1 条"));
    }

    @Test
    void rejectsBatchLargerThanFiftyRows() {
        List<JobImportDTO> jobs = new ArrayList<>();
        for (int index = 0; index < 51; index++) jobs.add(validJob("id-" + index));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.importJobs(jobs));

        assertEquals(400, exception.getCode());
        assertEquals("每次必须导入 1 到 50 个岗位", exception.getMessage());
    }

    @Test
    void keepsDatabaseResultWhenElasticsearchIsUnavailable() {
        when(jobMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        when(jobMapper.insert(any(Job.class))).thenAnswer(invocation -> {
            invocation.<Job>getArgument(0).setId(103L);
            return 1;
        });
        when(jobSearchRepository.saveAll(anyList()))
                .thenThrow(new IllegalStateException("elasticsearch offline"));

        JobImportResultVO result = service.importJobs(Collections.singletonList(validJob("abc123")));

        assertEquals(1, result.getCreated());
        verify(jobMapper).insert(any(Job.class));
    }

    @Test
    void importsTransactionallyAndEvictsBothJobCaches() throws Exception {
        Method method = JobImportService.class.getMethod("importJobs", List.class);

        assertNotNull(method.getAnnotation(Transactional.class));
        CacheEvict cacheEvict = method.getAnnotation(CacheEvict.class);
        assertNotNull(cacheEvict);
        assertArrayEquals(new String[]{"jobs", "job-detail"}, cacheEvict.cacheNames());
        assertTrue(cacheEvict.allEntries());
    }

    private JobImportDTO validJob(String externalId) {
        JobImportDTO job = new JobImportDTO();
        job.setSource("BOSS");
        job.setExternalId(externalId);
        job.setTitle("Java 开发工程师");
        job.setCompany("海纳科技");
        job.setCity("威海");
        job.setSalaryMin(10000);
        job.setSalaryMax(16000);
        job.setSalaryText("10-16K");
        job.setExperienceYears(1);
        job.setEducation("本科");
        job.setDescription("负责后端服务");
        job.setRequirements("Java Spring Boot");
        job.setWelfareTags("双休,五险一金");
        job.setSourceUrl("https://www.zhipin.com/job_detail/" + externalId + ".html");
        job.setStatus("OPEN");
        return job;
    }
}
