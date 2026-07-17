package com.relayflow.framework.web.handler;

import com.relayflow.common.exception.GlobalErrorCodeConstants;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.CommonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ServiceException.class)
    @ResponseStatus(HttpStatus.OK)
    public CommonResult<Object> handleServiceException(ServiceException exception) {
        return CommonResult.error(exception);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleValidationException(Exception exception) {
        FieldError fieldError = extractFieldError(exception);
        String message = "请求参数不合法";
        if (fieldError != null) {
            String field = fieldError.getField();
            String detail = fieldError.getDefaultMessage();
            message = field + ": " + (detail != null ? detail : "不合法");
            log.warn("Request validation failed: field={}, message={}", field, detail);
        } else {
            log.warn("Request validation failed", exception);
        }
        return CommonResult.error(400, message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResult<Void> handleException(Exception exception) {
        log.error("Unhandled exception", exception);
        return CommonResult.error(
                GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR.getCode(),
                "系统繁忙，请稍后重试");
    }

    private static FieldError extractFieldError(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException manv
                && manv.getBindingResult().getFieldError() != null) {
            return manv.getBindingResult().getFieldError();
        }
        if (exception instanceof BindException bind && bind.getBindingResult().getFieldError() != null) {
            return bind.getBindingResult().getFieldError();
        }
        return null;
    }
}
