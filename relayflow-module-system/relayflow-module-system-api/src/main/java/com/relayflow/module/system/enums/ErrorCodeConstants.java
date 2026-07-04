package com.relayflow.module.system.enums;

import com.relayflow.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCodeConstants implements ErrorCode {

    AUTH_LOGIN_BAD_CREDENTIALS(1_001_001_001, "用户名或密码错误"),
    AUTH_LOGIN_USER_DISABLED(1_001_001_002, "账号不可用"),
    USER_NOT_FOUND(1_001_002_001, "用户不存在"),
    TENANT_NOT_FOUND(1_001_003_001, "租户不存在");

    private final Integer code;
    private final String msg;
}
