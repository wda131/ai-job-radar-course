package cn.sdu.radar.service;

import cn.sdu.radar.ai.dto.AiEmbeddingRequest;
import cn.sdu.radar.ai.dto.AiEmbeddingResponse;
import cn.sdu.radar.ai.dto.AiEvaluationRequest;
import cn.sdu.radar.ai.dto.AiEvaluationResponse;
import cn.sdu.radar.ai.dto.AiMatchRequest;
import cn.sdu.radar.ai.dto.AiMatchResponse;
import cn.sdu.radar.ai.dto.AiQuestionRequest;
import cn.sdu.radar.ai.dto.AiQuestionResponse;

public interface AiService {
    boolean health();

    AiEmbeddingResponse embedding(AiEmbeddingRequest request);

    AiMatchResponse match(AiMatchRequest request);

    AiQuestionResponse questions(AiQuestionRequest request);

    AiEvaluationResponse evaluate(AiEvaluationRequest request);
}
