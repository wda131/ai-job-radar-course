package cn.sdu.radar.advice;

import cn.sdu.radar.exception.BusinessException;
import cn.sdu.radar.utils.CommonResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionAdviceTest {

    private final GlobalExceptionAdvice advice = new GlobalExceptionAdvice();

    @Test
    void keepsBusinessErrorCodeAndMessage() {
        CommonResult<Void> result = advice.handleBusinessException(
                new BusinessException(409, "duplicate"));

        assertEquals(409, result.getCode());
        assertEquals("duplicate", result.getMessage());
    }

    @Test
    void mapsIllegalArgumentToBadRequest() {
        CommonResult<Void> result = advice.handleIllegalArgument(
                new IllegalArgumentException("invalid"));

        assertEquals(400, result.getCode());
        assertEquals("invalid", result.getMessage());
    }
}
