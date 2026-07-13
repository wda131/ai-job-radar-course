package cn.sdu.radar.feign;

import cn.sdu.radar.utils.CommonResult;
import cn.sdu.radar.vo.JobSummaryVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("job-service")
public interface JobClient {
    @GetMapping("/api/jobs/internal/{id}")
    CommonResult<JobSummaryVO> getJob(@PathVariable("id") Long id);
}
