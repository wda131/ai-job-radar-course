package cn.sdu.radar.advice;

import cn.sdu.radar.exception.BusinessException;
import cn.sdu.radar.utils.CommonResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionAdvice {

    @ExceptionHandler(BusinessException.class)
    public CommonResult<Void> handleBusinessException(BusinessException exception) {
        return CommonResult.fail(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public CommonResult<Void> handleIllegalArgument(IllegalArgumentException exception) {
        return CommonResult.fail(400, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public CommonResult<Void> handleException(Exception exception) {
        log.error("Unhandled request exception", exception);
        return CommonResult.fail(500, "系统暂时不可用，请稍后重试");
    }
}
