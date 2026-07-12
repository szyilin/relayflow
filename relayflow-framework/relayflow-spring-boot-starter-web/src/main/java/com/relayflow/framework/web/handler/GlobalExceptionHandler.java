package com.relayflow.framework.web.handler;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.CommonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final int SYSTEM_ERROR_CODE = 2_000_001_001;

    @ExceptionHandler(ServiceException.class)
    @ResponseStatus(HttpStatus.OK)
    public CommonResult<Object> handleServiceException(ServiceException exception) {
        return CommonResult.error(exception);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleValidationException(Exception exception) {
        return CommonResult.error(400, "请求参数不合法");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResult<Void> handleException(Exception exception) {
        log.error("Unhandled exception", exception);
        return CommonResult.error(SYSTEM_ERROR_CODE, "系统繁忙，请稍后重试");
    }
}
