package com.relayflow.common.exception;

import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {

    private final Integer code;
    private final Object data;

    public ServiceException(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMsg(), null);
    }

    public ServiceException(ErrorCode errorCode, Object data) {
        this(errorCode.getCode(), errorCode.getMsg(), data);
    }

    public ServiceException(Integer code, String message) {
        this(code, message, null);
    }

    public ServiceException(Integer code, String message, Object data) {
        super(message);
        this.code = code;
        this.data = data;
    }
}
