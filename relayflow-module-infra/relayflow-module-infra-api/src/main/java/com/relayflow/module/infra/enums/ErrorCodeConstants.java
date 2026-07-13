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
    STORAGE_PROVIDER_CONFIG_INVALID(2_002_001_005, "存储配置不完整"),

    FILE_UPLOAD_SESSION_NOT_FOUND(2_002_002_001, "上传会话不存在"),
    FILE_UPLOAD_SESSION_EXPIRED(2_002_002_002, "上传会话已过期"),
    FILE_UPLOAD_SESSION_INVALID_STATUS(2_002_002_003, "上传会话状态无效"),
    FILE_UPLOAD_OBJECT_NOT_FOUND(2_002_002_004, "对象存储中未找到已上传对象"),
    FILE_UPLOAD_SIZE_MISMATCH(2_002_002_005, "确认时文件大小不匹配"),
    FILE_NOT_FOUND(2_002_002_006, "文件不存在"),
    FILE_UPLOAD_INVALID_REQUEST(2_002_002_007, "上传请求参数无效"),
    FILE_ACCESS_DENIED(2_002_002_008, "无权访问该文件"),

    NOTIFY_RECEIVER_REQUIRED(2_002_003_001, "通知接收人不能为空"),
    NOTIFY_TENANT_REQUIRED(2_002_003_002, "通知租户不能为空"),
    NOTIFY_TYPE_REQUIRED(2_002_003_003, "通知类型不能为空"),
    NOTIFY_TITLE_REQUIRED(2_002_003_004, "通知标题不能为空"),
    NOTIFY_PAYLOAD_INVALID(2_002_003_005, "通知载荷格式无效"),
    NOTIFY_NOT_FOUND(2_002_003_006, "通知不存在"),
    NOTIFY_LOGIN_REQUIRED(2_002_003_007, "请先登录"),

    SEARCH_KEYWORD_REQUIRED(2_002_004_001, "请输入搜索关键词");

    private final Integer code;
    private final String msg;
}
