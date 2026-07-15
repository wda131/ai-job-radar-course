package cn.sdu.radar.ai;

import cn.sdu.radar.ai.dto.AiEvaluationResponse;
import cn.sdu.radar.ai.dto.AiMatchResponse;
import cn.sdu.radar.ai.dto.AiQuestionResponse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AiContractsTest {

    @Test
    void responsesDefaultToUnavailable() {
        assertFalse(new AiMatchResponse().isAvailable());
        assertFalse(new AiQuestionResponse().isAvailable());
        assertFalse(new AiEvaluationResponse().isAvailable());
    }

    @Test
    void questionResponseKeepsFourQuestionsInOrder() {
        AiQuestionResponse response = new AiQuestionResponse();
        response.setQuestions(Arrays.asList("自我介绍", "项目经历", "技术难点", "职业规划"));

        assertEquals(4, response.getQuestions().size());
        assertEquals("自我介绍", response.getQuestions().get(0));
        assertEquals("职业规划", response.getQuestions().get(3));
    }
}
