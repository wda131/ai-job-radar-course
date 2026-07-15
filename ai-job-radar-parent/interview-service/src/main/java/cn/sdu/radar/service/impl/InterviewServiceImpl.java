package cn.sdu.radar.service.impl;

import cn.sdu.radar.ai.dto.AiEvaluationRequest;
import cn.sdu.radar.ai.dto.AiEvaluationResponse;
import cn.sdu.radar.ai.dto.AiQuestionRequest;
import cn.sdu.radar.ai.dto.AiQuestionResponse;
import cn.sdu.radar.exception.BusinessException;
import cn.sdu.radar.feign.AiClient;
import cn.sdu.radar.feign.JobClient;
import cn.sdu.radar.mapper.InterviewAnswerMapper;
import cn.sdu.radar.mapper.InterviewQuestionMapper;
import cn.sdu.radar.mapper.InterviewSessionMapper;
import cn.sdu.radar.pojo.InterviewAnswer;
import cn.sdu.radar.pojo.InterviewQuestion;
import cn.sdu.radar.pojo.InterviewSession;
import cn.sdu.radar.pojo.dto.InterviewAnswerDTO;
import cn.sdu.radar.pojo.vo.InterviewAnswerVO;
import cn.sdu.radar.pojo.vo.InterviewQuestionVO;
import cn.sdu.radar.pojo.vo.InterviewSessionVO;
import cn.sdu.radar.service.InterviewService;
import cn.sdu.radar.utils.CommonResult;
import cn.sdu.radar.vo.JobSummaryVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InterviewServiceImpl implements InterviewService {
    private final InterviewSessionMapper sessionMapper;
    private final InterviewQuestionMapper questionMapper;
    private final InterviewAnswerMapper answerMapper;
    private final JobClient jobClient;
    private final AiClient aiClient;

    @Autowired
    public InterviewServiceImpl(InterviewSessionMapper sessionMapper,
                                InterviewQuestionMapper questionMapper,
                                InterviewAnswerMapper answerMapper,
                                JobClient jobClient,
                                AiClient aiClient) {
        this.sessionMapper = sessionMapper;
        this.questionMapper = questionMapper;
        this.answerMapper = answerMapper;
        this.jobClient = jobClient;
        this.aiClient = aiClient;
    }

    @Override
    public InterviewSessionVO create(Long userId, Long jobId) {
        JobSummaryVO job = requireJob(jobId);
        LocalDateTime now = LocalDateTime.now();
        InterviewSession session = new InterviewSession();
        session.setUserId(userId);
        session.setJobId(jobId);
        session.setStatus("IN_PROGRESS");
        session.setTotalScore(0);
        session.setCreatedAt(now);
        session.setUpdatedAt(now);
        sessionMapper.insert(session);

        List<InterviewQuestion> questions = createQuestions(session.getId(), job);
        questions.forEach(questionMapper::insert);
        return toVO(session, job, questions, Collections.emptyList());
    }

    @Override
    public InterviewAnswerVO answer(Long userId, Long sessionId, InterviewAnswerDTO input) {
        if (input == null || input.getQuestionId() == null
                || !StringUtils.hasText(input.getAnswer())) {
            throw new BusinessException(400, "请选择问题并填写回答");
        }
        InterviewSession session = requireSession(userId, sessionId);
        if ("COMPLETED".equals(session.getStatus())) {
            throw new BusinessException(409, "本次模拟面试已完成");
        }
        InterviewQuestion question = questionMapper.selectById(input.getQuestionId());
        if (question == null || !sessionId.equals(question.getSessionId())) {
            throw new BusinessException(404, "面试问题不存在");
        }
        Integer duplicate = answerMapper.selectCount(new QueryWrapper<InterviewAnswer>()
                .eq("session_id", sessionId).eq("question_id", input.getQuestionId()));
        if (duplicate != null && duplicate > 0) {
            throw new BusinessException(409, "该问题已经回答");
        }

        JobSummaryVO job = requireJob(session.getJobId());
        AiEvaluationResponse ai = aiEvaluation(job, question, input.getAnswer());
        boolean aiUsed = validEvaluation(ai);
        int score = aiUsed ? ai.getScore()
                : calculateScore(input.getAnswer(), question.getReferenceKeywords());
        InterviewAnswer answer = new InterviewAnswer();
        answer.setSessionId(sessionId);
        answer.setQuestionId(input.getQuestionId());
        answer.setAnswer(input.getAnswer().trim());
        answer.setScore(score);
        answer.setFeedback(aiUsed ? ai.getSuggestion() : feedback(score));
        answer.setStrengths(aiUsed ? pack(ai.getStrengths()) : "");
        answer.setWeaknesses(aiUsed ? pack(ai.getWeaknesses()) : "");
        answer.setSuggestion(aiUsed ? ai.getSuggestion() : feedback(score));
        answer.setAiUsed(aiUsed);
        answer.setCreatedAt(LocalDateTime.now());
        answerMapper.insert(answer);
        completeSessionWhenReady(session);
        return toAnswerVO(answer);
    }

    @Override
    public InterviewSessionVO detail(Long userId, Long sessionId) {
        InterviewSession session = requireSession(userId, sessionId);
        return loadVO(session);
    }

    @Override
    public List<InterviewSessionVO> list(Long userId) {
        return sessionMapper.selectList(new QueryWrapper<InterviewSession>()
                        .eq("user_id", userId).orderByDesc("created_at"))
                .stream().map(this::loadVO).collect(Collectors.toList());
    }

    private InterviewSessionVO loadVO(InterviewSession session) {
        List<InterviewQuestion> questions = questionMapper.selectList(
                new QueryWrapper<InterviewQuestion>().eq("session_id", session.getId())
                        .orderByAsc("question_order"));
        List<InterviewAnswer> answers = answerMapper.selectList(
                new QueryWrapper<InterviewAnswer>().eq("session_id", session.getId()));
        return toVO(session, requireJob(session.getJobId()), questions, answers);
    }

    private InterviewSession requireSession(Long userId, Long sessionId) {
        InterviewSession session = sessionId == null ? null : sessionMapper.selectById(sessionId);
        if (session == null || !userId.equals(session.getUserId())) {
            throw new BusinessException(404, "模拟面试记录不存在");
        }
        return session;
    }

    private JobSummaryVO requireJob(Long jobId) {
        CommonResult<JobSummaryVO> result = jobClient.getJob(jobId);
        if (result == null || result.getCode() != 200 || result.getData() == null) {
            throw new BusinessException(503, "岗位服务暂不可用");
        }
        return result.getData();
    }

    private List<InterviewQuestion> createQuestions(Long sessionId, JobSummaryVO job) {
        List<String> aiQuestions = aiQuestions(job);
        if (aiQuestions.size() == 4) {
            List<InterviewQuestion> questions = new ArrayList<>();
            for (int i = 0; i < aiQuestions.size(); i++) {
                questions.add(question(sessionId, i + 1, aiQuestions.get(i), job.getRequirements()));
            }
            return questions;
        }
        List<InterviewQuestion> questions = new ArrayList<>();
        questions.add(question(sessionId, 1,
                "请做一个两分钟的自我介绍，并说明为什么应聘“" + job.getTitle() + "”。",
                job.getTitle()));
        questions.add(question(sessionId, 2,
                "请结合实际经历说明你对岗位核心技术的掌握情况。",
                job.getRequirements()));
        questions.add(question(sessionId, 3,
                "介绍一个最能体现你能力的项目，包括职责、难点和结果。",
                "项目,职责,难点,结果"));
        questions.add(question(sessionId, 4,
                "如果线上功能突然出现异常，你会如何定位并解决问题？",
                "分析,日志,方案,验证"));
        return questions;
    }

    private List<String> aiQuestions(JobSummaryVO job) {
        AiQuestionRequest request = new AiQuestionRequest();
        request.setJobId(job.getId());
        request.setJobTitle(job.getTitle());
        request.setJobDescription(job.getDescription());
        request.setJobSkills(job.getRequirements());
        try {
            CommonResult<AiQuestionResponse> result = aiClient.questions(request);
            AiQuestionResponse response = result != null && result.getCode() == 200
                    ? result.getData() : null;
            if (response == null || !response.isAvailable() || response.getQuestions() == null
                    || response.getQuestions().size() != 4
                    || response.getQuestions().stream().anyMatch(item -> !StringUtils.hasText(item))) {
                return Collections.emptyList();
            }
            return response.getQuestions();
        } catch (RuntimeException exception) {
            return Collections.emptyList();
        }
    }

    private AiEvaluationResponse aiEvaluation(JobSummaryVO job, InterviewQuestion question,
                                              String answer) {
        AiEvaluationRequest request = new AiEvaluationRequest();
        request.setJobTitle(job.getTitle());
        request.setJobSkills(job.getRequirements());
        request.setQuestion(question.getQuestion());
        request.setAnswer(answer);
        try {
            CommonResult<AiEvaluationResponse> result = aiClient.evaluate(request);
            return result != null && result.getCode() == 200 ? result.getData() : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private boolean validEvaluation(AiEvaluationResponse response) {
        return response != null && response.isAvailable() && response.getScore() != null
                && response.getScore() >= 0 && response.getScore() <= 100;
    }

    private InterviewQuestion question(Long sessionId, int order, String text, String keywords) {
        InterviewQuestion question = new InterviewQuestion();
        question.setSessionId(sessionId);
        question.setQuestionOrder(order);
        question.setQuestion(text);
        question.setReferenceKeywords(keywords == null ? "" : keywords);
        return question;
    }

    private int calculateScore(String answer, String keywordText) {
        if (!StringUtils.hasText(keywordText)) {
            return 60;
        }
        String normalizedAnswer = answer.toLowerCase(Locale.ROOT);
        int total = 0;
        int hits = 0;
        for (String part : keywordText.split("[,，、;/；\\n]")) {
            String keyword = part.trim().toLowerCase(Locale.ROOT);
            if (StringUtils.hasText(keyword)) {
                total++;
                if (normalizedAnswer.contains(keyword)) {
                    hits++;
                }
            }
        }
        return total == 0 ? 60 : 30 + (int) Math.round(70.0 * hits / total);
    }

    private String feedback(int score) {
        if (score >= 85) {
            return "回答充分，覆盖了主要评价要点。";
        }
        if (score >= 60) {
            return "回答覆盖了部分要点，可以补充更具体的项目细节。";
        }
        return "建议补充具体案例，并按照背景、行动、结果组织回答。";
    }

    private String pack(List<String> values) {
        return values == null ? "" : values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .collect(Collectors.joining("\n"));
    }

    private List<String> unpack(String value) {
        if (!StringUtils.hasText(value)) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        for (String item : value.split("\\n")) {
            if (StringUtils.hasText(item)) {
                values.add(item.trim());
            }
        }
        return values;
    }

    private void completeSessionWhenReady(InterviewSession session) {
        Integer questionCount = questionMapper.selectCount(
                new QueryWrapper<InterviewQuestion>().eq("session_id", session.getId()));
        Integer answerCount = answerMapper.selectCount(
                new QueryWrapper<InterviewAnswer>().eq("session_id", session.getId()));
        if (questionCount != null && questionCount > 0 && questionCount.equals(answerCount)) {
            List<InterviewAnswer> answers = answerMapper.selectList(
                    new QueryWrapper<InterviewAnswer>().eq("session_id", session.getId()));
            int average = answers.isEmpty() ? 0 : (int) Math.round(
                    answers.stream().mapToInt(InterviewAnswer::getScore).average().orElse(0));
            session.setStatus("COMPLETED");
            session.setTotalScore(average);
            session.setUpdatedAt(LocalDateTime.now());
            sessionMapper.updateById(session);
        }
    }

    private InterviewSessionVO toVO(InterviewSession session, JobSummaryVO job,
                                    List<InterviewQuestion> questions,
                                    List<InterviewAnswer> answers) {
        Map<Long, InterviewAnswer> answerMap = new HashMap<>();
        for (InterviewAnswer answer : answers) {
            answerMap.put(answer.getQuestionId(), answer);
        }
        List<InterviewQuestionVO> questionVOs = questions.stream().map(question -> {
            InterviewQuestionVO vo = new InterviewQuestionVO();
            vo.setId(question.getId());
            vo.setQuestionOrder(question.getQuestionOrder());
            vo.setQuestion(question.getQuestion());
            InterviewAnswer answer = answerMap.get(question.getId());
            if (answer != null) {
                vo.setAnswer(toAnswerVO(answer));
            }
            return vo;
        }).collect(Collectors.toList());

        InterviewSessionVO vo = new InterviewSessionVO();
        vo.setId(session.getId());
        vo.setStatus(session.getStatus());
        vo.setTotalScore(session.getTotalScore());
        vo.setJob(job);
        vo.setQuestions(questionVOs);
        vo.setCreatedAt(session.getCreatedAt());
        return vo;
    }

    private InterviewAnswerVO toAnswerVO(InterviewAnswer answer) {
        InterviewAnswerVO vo = new InterviewAnswerVO();
        vo.setId(answer.getId());
        vo.setQuestionId(answer.getQuestionId());
        vo.setAnswer(answer.getAnswer());
        vo.setScore(answer.getScore());
        vo.setFeedback(answer.getFeedback());
        vo.setStrengths(unpack(answer.getStrengths()));
        vo.setWeaknesses(unpack(answer.getWeaknesses()));
        vo.setSuggestion(answer.getSuggestion());
        vo.setAiUsed(Boolean.TRUE.equals(answer.getAiUsed()));
        vo.setCreatedAt(answer.getCreatedAt());
        return vo;
    }
}
