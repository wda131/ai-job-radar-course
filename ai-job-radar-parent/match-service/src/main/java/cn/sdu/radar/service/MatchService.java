package cn.sdu.radar.service;

import cn.sdu.radar.pojo.vo.MatchReportVO;

import java.util.List;

public interface MatchService {
    MatchReportVO match(Long userId, Long jobId);

    List<MatchReportVO> history(Long userId);
}
