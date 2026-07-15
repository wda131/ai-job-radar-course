package cn.sdu.radar.controller;

import cn.sdu.radar.ai.dto.AiEmbeddingRequest;
import cn.sdu.radar.ai.dto.AiEmbeddingResponse;
import cn.sdu.radar.ai.dto.AiEvaluationRequest;
import cn.sdu.radar.ai.dto.AiEvaluationResponse;
import cn.sdu.radar.ai.dto.AiMatchRequest;
import cn.sdu.radar.ai.dto.AiMatchResponse;
import cn.sdu.radar.ai.dto.AiQuestionRequest;
import cn.sdu.radar.ai.dto.AiQuestionResponse;
import cn.sdu.radar.service.AiService;
import cn.sdu.radar.utils.CommonResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/internal/ai")
public class AiController {
    private final AiService aiService;

    @Autowired
    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/health")
    public CommonResult<Map<String, Boolean>> health() {
        return CommonResult.success(Collections.singletonMap("available", aiService.health()));
    }

    @PostMapping("/embeddings")
    public CommonResult<AiEmbeddingResponse> embeddings(@RequestBody AiEmbeddingRequest request) {
        return CommonResult.success(aiService.embedding(request));
    }

    @PostMapping("/match-explanation")
    public CommonResult<AiMatchResponse> match(@RequestBody AiMatchRequest request) {
        return CommonResult.success(aiService.match(request));
    }

    @PostMapping("/interview-questions")
    public CommonResult<AiQuestionResponse> questions(@RequestBody AiQuestionRequest request) {
        return CommonResult.success(aiService.questions(request));
    }

    @PostMapping("/interview-evaluation")
    public CommonResult<AiEvaluationResponse> evaluate(@RequestBody AiEvaluationRequest request) {
        return CommonResult.success(aiService.evaluate(request));
    }
}
