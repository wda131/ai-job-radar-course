package cn.sdu.radar.service;

import cn.sdu.radar.feign.JobClient;
import cn.sdu.radar.feign.UserClient;
import cn.sdu.radar.mapper.MatchResultMapper;
import cn.sdu.radar.pojo.vo.MatchReportVO;
import cn.sdu.radar.service.impl.MatchServiceImpl;
import cn.sdu.radar.utils.CommonResult;
import cn.sdu.radar.vo.JobSummaryVO;
import cn.sdu.radar.vo.UserProfileVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class MatchServiceImplTest {
    private UserClient userClient;
    private JobClient jobClient;
    private MatchResultMapper resultMapper;
    private MatchServiceImpl matchService;

    @BeforeEach
    void setUp() {
        userClient = mock(UserClient.class);
        jobClient = mock(JobClient.class);
        resultMapper = mock(MatchResultMapper.class);
        matchService = new MatchServiceImpl(userClient, jobClient, resultMapper);
    }

    @Test
    void calculatesExplainableScoreAndPersistsReport() {
        UserProfileVO profile = new UserProfileVO();
        profile.setId(1L);
        profile.setCity("威海");
        profile.setSkills("Java,Spring Boot,MySQL");
        profile.setExperienceYears(1);
        profile.setSalaryMin(6000);
        profile.setSalaryMax(10000);

        JobSummaryVO job = new JobSummaryVO();
        job.setId(2L);
        job.setTitle("Java后端开发");
        job.setCompany("海纳科技");
        job.setCity("威海");
        job.setRequirements("Java,Spring Boot,Nacos,MySQL");
        job.setExperienceYears(1);
        job.setSalaryMin(7000);
        job.setSalaryMax(9000);

        when(userClient.getProfile(1L)).thenReturn(CommonResult.success(profile));
        when(jobClient.getJob(2L)).thenReturn(CommonResult.success(job));

        MatchReportVO result = matchService.match(1L, 2L);

        assertEquals(85, result.getScore());
        assertTrue(result.getMatchedSkills().contains("Java"));
        assertTrue(result.getMissingSkills().contains("Nacos"));
        assertEquals("海纳科技", result.getJob().getCompany());
        verify(resultMapper).insert(any());
    }
}
