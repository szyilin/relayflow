package com.relayflow.module.infra.enums;

import com.relayflow.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCodeConstants implements ErrorCode {

    STORAGE_PROVIDER_NOT_FOUND(2_002_001_001, "存储 provider 不存在"),
    STORAGE_PROVIDER_REFERENCED(2_002_001_002, "存储 provider 仍被文件引用，无法删除"),
    STORAGE_TEST_CONNECTION_FAILED(2_002_001_003, "存储连通性测试失败"),
    STORAGE_PROVIDER_UNSUPPORTED(2_002_001_004, "不支持的存储 provider 类型"),
    STORAGE_PROVIDER_CONFIG_INVALID(2_002_001_005, "存储配置不完整");

    private final Integer code;
    private final String msg;
}
