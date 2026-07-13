package cn.sdu.radar.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CommonResultTest {

    @Test
    void buildsSuccessResponse() {
        CommonResult<String> result = CommonResult.success("ok");

        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals("ok", result.getData());
    }

    @Test
    void buildsFailureResponse() {
        CommonResult<Void> result = CommonResult.fail(404, "not found");

        assertEquals(404, result.getCode());
        assertEquals("not found", result.getMessage());
        assertNull(result.getData());
    }
}
