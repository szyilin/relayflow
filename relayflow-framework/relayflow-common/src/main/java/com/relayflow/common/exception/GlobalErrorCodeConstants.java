package com.relayflow.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 系统 / 框架级错误码（类型位 = 2，域模块 = 000）。
 */
@Getter
@RequiredArgsConstructor
public enum GlobalErrorCodeConstants implements ErrorCode {

    INTERNAL_SERVER_ERROR(2_000_001_001, "系统异常"),
    UNAUTHORIZED(2_000_001_002, "未登录或 Token 无效"),
    ;

    private final Integer code;
    private final String msg;
}
