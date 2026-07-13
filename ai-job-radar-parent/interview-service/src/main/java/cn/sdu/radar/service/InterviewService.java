package cn.sdu.radar.service;

import cn.sdu.radar.pojo.dto.InterviewAnswerDTO;
import cn.sdu.radar.pojo.vo.InterviewAnswerVO;
import cn.sdu.radar.pojo.vo.InterviewSessionVO;

import java.util.List;

public interface InterviewService {
    InterviewSessionVO create(Long userId, Long jobId);

    InterviewAnswerVO answer(Long userId, Long sessionId, InterviewAnswerDTO input);

    InterviewSessionVO detail(Long userId, Long sessionId);

    List<InterviewSessionVO> list(Long userId);
}
