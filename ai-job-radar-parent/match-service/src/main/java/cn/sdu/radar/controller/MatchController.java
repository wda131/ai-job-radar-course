package cn.sdu.radar.controller;

import cn.sdu.radar.auth.UserContext;
import cn.sdu.radar.pojo.vo.MatchReportVO;
import cn.sdu.radar.service.MatchService;
import cn.sdu.radar.utils.CommonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchController {
    private final MatchService matchService;

    @Autowired
    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping("/jobs/{jobId}")
    public CommonResult<MatchReportVO> match(@PathVariable Long jobId) {
        return CommonResult.success(matchService.match(UserContext.getUserId(), jobId));
    }

    @GetMapping
    public CommonResult<List<MatchReportVO>> history() {
        return CommonResult.success(matchService.history(UserContext.getUserId()));
    }
}
