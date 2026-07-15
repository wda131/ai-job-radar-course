package cn.sdu.radar.controller;

import cn.sdu.radar.pojo.dto.JobImportBatchDTO;
import cn.sdu.radar.pojo.vo.JobImportResultVO;
import cn.sdu.radar.service.JobImportService;
import cn.sdu.radar.service.JobService;
import cn.sdu.radar.utils.CommonResult;
import cn.sdu.radar.vo.JobSummaryVO;
import cn.sdu.radar.vo.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class JobController {
    private final JobService jobService;
    private final JobImportService jobImportService;

    @Autowired
    public JobController(JobService jobService, JobImportService jobImportService) {
        this.jobService = jobService;
        this.jobImportService = jobImportService;
    }

    @GetMapping
    public CommonResult<PageResult<JobSummaryVO>> search(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String city,
            @RequestParam(required = false) Integer minSalary,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "8") long size) {
        return CommonResult.success(jobService.search(keyword, city, minSalary, page, size));
    }

    @GetMapping("/{id}")
    public CommonResult<JobSummaryVO> detail(@PathVariable Long id) {
        return CommonResult.success(jobService.getById(id));
    }

    @GetMapping("/internal/{id}")
    public CommonResult<JobSummaryVO> internalDetail(@PathVariable Long id) {
        return CommonResult.success(jobService.getById(id));
    }

    @PostMapping("/import")
    public CommonResult<JobImportResultVO> importJobs(@RequestBody JobImportBatchDTO batch) {
        return CommonResult.success(jobImportService.importJobs(
                batch == null ? null : batch.getJobs()));
    }
}
