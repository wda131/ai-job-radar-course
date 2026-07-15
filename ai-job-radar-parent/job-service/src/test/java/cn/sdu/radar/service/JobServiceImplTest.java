package cn.sdu.radar.service;

import cn.sdu.radar.exception.BusinessException;
import cn.sdu.radar.mapper.JobMapper;
import cn.sdu.radar.pojo.Job;
import cn.sdu.radar.search.JobDocument;
import cn.sdu.radar.search.JobSearchService;
import cn.sdu.radar.service.impl.JobServiceImpl;
import cn.sdu.radar.vo.JobSummaryVO;
import cn.sdu.radar.vo.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.annotation.Cacheable;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobServiceImplTest {

    private JobMapper jobMapper;
    private JobSearchService jobSearchService;
    private JobServiceImpl jobService;
    private Job job;

    @BeforeEach
    void setUp() {
        jobMapper = mock(JobMapper.class);
        jobSearchService = mock(JobSearchService.class);
        jobService = new JobServiceImpl(jobMapper, jobSearchService);
        job = new Job();
        job.setId(10L);
        job.setTitle("Java后端开发实习生");
        job.setCompany("海纳科技");
        job.setCity("威海");
        job.setSalaryMin(5000);
        job.setSalaryMax(7000);
        job.setSalaryText("5-7K");
        job.setExperienceYears(0);
        job.setEducation("本科");
        job.setDescription("参与业务系统开发");
        job.setRequirements("Java,Spring Boot,MySQL");
        job.setWelfareTags("双休,导师制");
        job.setSource("BOSS");
        job.setSourceUrl("https://www.zhipin.com/job_detail/abc123.html");
        job.setStatus("OPEN");
        job.setPostedAt(LocalDateTime.now());
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchBuildsFiltersAndReturnsPage() {
        when(jobMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                .thenAnswer(invocation -> {
                    Page<Job> page = invocation.getArgument(0);
                    page.setRecords(Collections.singletonList(job));
                    page.setTotal(1);
                    return page;
                });

        PageResult<JobSummaryVO> result = jobService.search(
                "Java", "威海", 5000, 1, 8);

        assertEquals(1, result.getRecords().size());
        assertEquals("海纳科技", result.getRecords().get(0).getCompany());
        assertEquals("BOSS", result.getRecords().get(0).getSource());
        assertEquals(job.getSourceUrl(), result.getRecords().get(0).getSourceUrl());
        assertEquals("5-7K", result.getRecords().get(0).getSalaryText());
        assertEquals(1, result.getTotal());

        ArgumentCaptor<QueryWrapper<Job>> wrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(jobMapper).selectPage(any(Page.class), wrapperCaptor.capture());
        String sql = wrapperCaptor.getValue().getSqlSegment();
        assertTrue(sql.contains("title"));
        assertTrue(sql.contains("company"));
        assertTrue(sql.contains("city"));
        assertTrue(sql.contains("salary_min"));
    }

    @Test
    void searchRejectsInvalidPagination() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> jobService.search("", "", null, 0, 100));

        assertEquals(400, exception.getCode());
    }

    @Test
    void getByIdRejectsMissingJob() {
        when(jobMapper.selectById(99L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> jobService.getById(99L));

        assertEquals(404, exception.getCode());
    }

    @Test
    void cachesJobSearchAndDetailWithSeparateCacheNames() throws Exception {
        Method search = JobServiceImpl.class.getMethod("search", String.class, String.class,
                Integer.class, long.class, long.class);
        Method detail = JobServiceImpl.class.getMethod("getById", Long.class);

        assertEquals("jobs", search.getAnnotation(Cacheable.class).cacheNames()[0]);
        assertEquals("job-detail", detail.getAnnotation(Cacheable.class).cacheNames()[0]);
    }

    @Test
    void keywordSearchUsesElasticsearchWhenAvailable() {
        JobSummaryVO summary = new JobSummaryVO();
        summary.setId(10L);
        summary.setTitle("Java后端开发实习生");
        summary.setSource("BOSS");
        summary.setSourceUrl("https://www.zhipin.com/job_detail/abc123.html");
        PageResult<JobSummaryVO> elasticsearchResult = new PageResult<>(
                Collections.singletonList(summary), 1, 1, 8);
        when(jobSearchService.search("Java", "", null, 1, 8))
                .thenReturn(elasticsearchResult);

        PageResult<JobSummaryVO> result = jobService.search("Java", "", null, 1, 8);

        assertEquals("Java后端开发实习生", result.getRecords().get(0).getTitle());
        assertEquals("BOSS", result.getRecords().get(0).getSource());
        assertEquals(summary.getSourceUrl(), result.getRecords().get(0).getSourceUrl());
        verify(jobMapper, never()).selectPage(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void elasticsearchFailureFallsBackToMybatis() {
        when(jobSearchService.search("Java", "", null, 1, 8))
                .thenThrow(new IllegalStateException("elasticsearch offline"));
        when(jobMapper.selectPage(any(Page.class), any(QueryWrapper.class)))
                .thenAnswer(invocation -> {
                    Page<Job> page = invocation.getArgument(0);
                    page.setRecords(Collections.singletonList(job));
                    page.setTotal(1);
                    return page;
                });

        PageResult<JobSummaryVO> result = jobService.search("Java", "", null, 1, 8);

        assertEquals(1, result.getTotal());
        verify(jobMapper).selectPage(any(Page.class), any(QueryWrapper.class));
    }

    @Test
    void elasticsearchDocumentPreservesImportedJobSource() {
        JobSummaryVO summary = JobDocument.from(job).toSummary();

        assertEquals("BOSS", summary.getSource());
        assertEquals(job.getSourceUrl(), summary.getSourceUrl());
        assertEquals("5-7K", summary.getSalaryText());
    }
}
