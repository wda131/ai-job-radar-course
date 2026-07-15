package cn.sdu.radar.feign;

import cn.sdu.radar.ai.dto.AiEvaluationRequest;
import cn.sdu.radar.ai.dto.AiEvaluationResponse;
import cn.sdu.radar.ai.dto.AiQuestionRequest;
import cn.sdu.radar.ai.dto.AiQuestionResponse;
import cn.sdu.radar.utils.CommonResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("ai-service")
public interface AiClient {
    @PostMapping("/internal/ai/interview-questions")
    CommonResult<AiQuestionResponse> questions(@RequestBody AiQuestionRequest request);

    @PostMapping("/internal/ai/interview-evaluation")
    CommonResult<AiEvaluationResponse> evaluate(@RequestBody AiEvaluationRequest request);
}
