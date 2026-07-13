package cn.sdu.radar.controller;

import cn.sdu.radar.auth.UserContext;
import cn.sdu.radar.pojo.dto.ApplicationCreateDTO;
import cn.sdu.radar.pojo.dto.ApplicationUpdateDTO;
import cn.sdu.radar.pojo.vo.ApplicationVO;
import cn.sdu.radar.service.ApplicationRecordService;
import cn.sdu.radar.utils.CommonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {
    private final ApplicationRecordService applicationService;

    @Autowired
    public ApplicationController(ApplicationRecordService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping
    public CommonResult<List<ApplicationVO>> list() {
        return CommonResult.success(applicationService.list(UserContext.getUserId()));
    }

    @PostMapping
    public CommonResult<ApplicationVO> create(@RequestBody ApplicationCreateDTO input) {
        return CommonResult.success(applicationService.create(UserContext.getUserId(), input));
    }

    @PutMapping("/{id}/status")
    public CommonResult<ApplicationVO> update(@PathVariable Long id,
                                               @RequestBody ApplicationUpdateDTO input) {
        return CommonResult.success(applicationService.update(UserContext.getUserId(), id, input));
    }
}
