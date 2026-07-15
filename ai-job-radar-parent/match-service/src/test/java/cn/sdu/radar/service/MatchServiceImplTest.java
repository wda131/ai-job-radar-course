package cn.sdu.radar.service;

import cn.sdu.radar.ai.dto.AiMatchResponse;
import cn.sdu.radar.feign.AiClient;
import cn.sdu.radar.feign.JobClient;
import cn.sdu.radar.feign.UserClient;
import cn.sdu.radar.mapper.MatchResultMapper;
import cn.sdu.radar.pojo.MatchResult;
import cn.sdu.radar.pojo.vo.MatchReportVO;
import cn.sdu.radar.service.impl.MatchServiceImpl;
import cn.sdu.radar.utils.CommonResult;
import cn.sdu.radar.vo.JobSummaryVO;
import cn.sdu.radar.vo.UserProfileVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class MatchServiceImplTest {
    private UserClient userClient;
    private JobClient jobClient;
    private AiClient aiClient;
    private MatchResultMapper resultMapper;
    private MatchServiceImpl matchService;

    @BeforeEach
    void setUp() {
        userClient = mock(UserClient.class);
        jobClient = mock(JobClient.class);
        aiClient = mock(AiClient.class);
        resultMapper = mock(MatchResultMapper.class);
        matchService = new MatchServiceImpl(userClient, jobClient, aiClient, resultMapper);
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

    @Test
    void blendsRuleAndSemanticScoresWhenAiIsAvailable() {
        prepareMatchData();
        AiMatchResponse ai = new AiMatchResponse();
        ai.setAvailable(true);
        ai.setSemanticScore(95);
        ai.setSummary("技能基础匹配，建议补强分布式经验");
        ai.setStrengths(Arrays.asList("Java基础", "Spring Boot项目"));
        ai.setGaps(Arrays.asList("Nacos经验"));
        ai.setSuggestions(Arrays.asList("补充微服务项目"));
        when(aiClient.explain(any())).thenReturn(CommonResult.success(ai));

        MatchReportVO result = matchService.match(1L, 2L);

        assertEquals(88, result.getScore());
        assertEquals(85, result.getRuleScore());
        assertEquals(95, result.getSemanticScore());
        assertTrue(result.isAiUsed());
        assertEquals("Java基础", result.getStrengths().get(0));
    }

    @Test
    void preservesRuleScoreWhenAiIsUnavailable() {
        prepareMatchData();
        when(aiClient.explain(any())).thenThrow(new IllegalStateException("ai-service offline"));

        MatchReportVO result = matchService.match(1L, 2L);

        assertEquals(85, result.getScore());
        assertEquals(85, result.getRuleScore());
        assertEquals(null, result.getSemanticScore());
        assertTrue(!result.isAiUsed());
        ArgumentCaptor<MatchResult> captor = ArgumentCaptor.forClass(MatchResult.class);
        verify(resultMapper).insert(captor.capture());
        assertEquals(85, captor.getValue().getScore());
    }

    private void prepareMatchData() {
        UserProfileVO profile = new UserProfileVO();
        profile.setId(1L);
        profile.setCity("威海");
        profile.setSkills("Java,Spring Boot,MySQL");
        profile.setIntroduction("完成过后端项目");
        profile.setExperienceYears(1);
        profile.setSalaryMin(6000);
        profile.setSalaryMax(10000);

        JobSummaryVO job = new JobSummaryVO();
        job.setId(2L);
        job.setTitle("Java后端开发");
        job.setCompany("海纳科技");
        job.setCity("威海");
        job.setDescription("负责微服务接口开发");
        job.setRequirements("Java,Spring Boot,Nacos,MySQL");
        job.setExperienceYears(1);
        job.setSalaryMin(7000);
        job.setSalaryMax(9000);

        when(userClient.getProfile(1L)).thenReturn(CommonResult.success(profile));
        when(jobClient.getJob(2L)).thenReturn(CommonResult.success(job));
    }
}
