package cn.sdu.radar.service;

import cn.sdu.radar.feign.JobClient;
import cn.sdu.radar.mapper.InterviewAnswerMapper;
import cn.sdu.radar.mapper.InterviewQuestionMapper;
import cn.sdu.radar.mapper.InterviewSessionMapper;
import cn.sdu.radar.pojo.InterviewAnswer;
import cn.sdu.radar.pojo.InterviewQuestion;
import cn.sdu.radar.pojo.InterviewSession;
import cn.sdu.radar.pojo.dto.InterviewAnswerDTO;
import cn.sdu.radar.pojo.vo.InterviewAnswerVO;
import cn.sdu.radar.pojo.vo.InterviewSessionVO;
import cn.sdu.radar.service.impl.InterviewServiceImpl;
import cn.sdu.radar.utils.CommonResult;
import cn.sdu.radar.vo.JobSummaryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterviewServiceImplTest {
    private InterviewSessionMapper sessionMapper;
    private InterviewQuestionMapper questionMapper;
    private InterviewAnswerMapper answerMapper;
    private JobClient jobClient;
    private InterviewServiceImpl interviewService;
    private JobSummaryVO job;

    @BeforeEach
    void setUp() {
        sessionMapper = mock(InterviewSessionMapper.class);
        questionMapper = mock(InterviewQuestionMapper.class);
        answerMapper = mock(InterviewAnswerMapper.class);
        jobClient = mock(JobClient.class);
        interviewService = new InterviewServiceImpl(
                sessionMapper, questionMapper, answerMapper, jobClient);
        job = new JobSummaryVO();
        job.setId(5L);
        job.setTitle("Java后端开发");
        job.setCompany("海纳科技");
        job.setRequirements("Java,Spring Boot,MySQL");
        when(jobClient.getJob(5L)).thenReturn(CommonResult.success(job));
    }

    @Test
    void createsFourOrderedQuestions() {
        InterviewSessionVO result = interviewService.create(1L, 5L);

        ArgumentCaptor<InterviewQuestion> captor = ArgumentCaptor.forClass(InterviewQuestion.class);
        verify(questionMapper, org.mockito.Mockito.times(4)).insert(captor.capture());
        assertEquals(4, captor.getAllValues().size());
        assertEquals(1, captor.getAllValues().get(0).getQuestionOrder());
        assertEquals(4, captor.getAllValues().get(3).getQuestionOrder());
        assertTrue(captor.getAllValues().get(0).getQuestion().contains("Java后端开发"));
        assertEquals("IN_PROGRESS", result.getStatus());
    }

    @Test
    void scoresAnswerFromReferenceKeywords() {
        InterviewSession session = new InterviewSession();
        session.setId(7L);
        session.setUserId(1L);
        session.setJobId(5L);
        session.setStatus("IN_PROGRESS");
        InterviewQuestion question = new InterviewQuestion();
        question.setId(9L);
        question.setSessionId(7L);
        question.setReferenceKeywords("Java,Spring Boot,MySQL");
        when(sessionMapper.selectById(7L)).thenReturn(session);
        when(questionMapper.selectById(9L)).thenReturn(question);
        when(answerMapper.selectCount(any())).thenReturn(0);
        when(questionMapper.selectCount(any())).thenReturn(4);
        when(answerMapper.selectList(any())).thenReturn(Arrays.asList(
                answer(80), answer(70), answer(90), answer(60)));
        InterviewAnswerDTO input = new InterviewAnswerDTO();
        input.setQuestionId(9L);
        input.setAnswer("我使用 Java 和 Spring Boot 开发接口，并用 MySQL 保存业务数据。");

        InterviewAnswerVO result = interviewService.answer(1L, 7L, input);

        assertEquals(100, result.getScore());
        assertTrue(result.getFeedback().contains("充分"));
        verify(answerMapper).insert(any(InterviewAnswer.class));
    }

    private InterviewAnswer answer(int score) {
        InterviewAnswer answer = new InterviewAnswer();
        answer.setScore(score);
        return answer;
    }
}
