package cn.sdu.radar.feign;

import cn.sdu.radar.ai.dto.AiMatchRequest;
import cn.sdu.radar.ai.dto.AiMatchResponse;
import cn.sdu.radar.utils.CommonResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("ai-service")
public interface AiClient {
    @PostMapping("/internal/ai/match-explanation")
    CommonResult<AiMatchResponse> explain(@RequestBody AiMatchRequest request);
}
