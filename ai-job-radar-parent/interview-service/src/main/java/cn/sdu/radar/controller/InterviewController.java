package cn.sdu.radar.controller;

import cn.sdu.radar.auth.UserContext;
import cn.sdu.radar.pojo.dto.InterviewAnswerDTO;
import cn.sdu.radar.pojo.vo.InterviewAnswerVO;
import cn.sdu.radar.pojo.vo.InterviewSessionVO;
import cn.sdu.radar.service.InterviewService;
import cn.sdu.radar.utils.CommonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {
    private final InterviewService interviewService;

    @Autowired
    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @PostMapping
    public CommonResult<InterviewSessionVO> create(@RequestParam Long jobId) {
        return CommonResult.success(interviewService.create(UserContext.getUserId(), jobId));
    }

    @GetMapping
    public CommonResult<List<InterviewSessionVO>> list() {
        return CommonResult.success(interviewService.list(UserContext.getUserId()));
    }

    @GetMapping("/{id}")
    public CommonResult<InterviewSessionVO> detail(@PathVariable Long id) {
        return CommonResult.success(interviewService.detail(UserContext.getUserId(), id));
    }

    @PostMapping("/{id}/answers")
    public CommonResult<InterviewAnswerVO> answer(@PathVariable Long id,
                                                   @RequestBody InterviewAnswerDTO input) {
        return CommonResult.success(interviewService.answer(UserContext.getUserId(), id, input));
    }
}
