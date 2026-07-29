package com.relayflow.module.agent.enums;

import com.relayflow.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Agent domain error codes — module segment {@code 009}.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCodeConstants implements ErrorCode {

    LLM_CONNECTION_NOT_FOUND(1_009_001_001, "模型连接不存在"),
    LLM_CONNECTION_CODE_DUPLICATE(1_009_001_002, "模型引用编码已存在"),
    LLM_CONNECTION_INVALID(1_009_001_003, "模型连接参数无效"),
    LLM_CONNECTION_TEST_FAILED(1_009_001_004, "模型连通性测试失败"),
    LLM_CONNECTION_UNSUPPORTED(1_009_001_005, "不支持的模型提供方类型"),
    LLM_CONNECTION_DISABLED(1_009_001_006, "模型连接未启用"),

    AGENT_DEF_NOT_FOUND(1_009_002_001, "智能体不存在"),
    AGENT_DEF_FORBIDDEN(1_009_002_002, "无权操作该智能体"),
    AGENT_DEF_INVALID(1_009_002_003, "智能体参数无效"),
    AGENT_DEF_DISABLED(1_009_002_004, "智能体已停用"),
    AGENT_SKILL_UNKNOWN(1_009_002_005, "未知的技能编码"),
    AGENT_CAPABILITY_PACK_UNKNOWN(1_009_002_006, "未知的能力包"),
    AGENT_CAPABILITY_DOMAIN_UNKNOWN(1_009_002_007, "未知的能力域"),

    AGENT_SESSION_NOT_FOUND(1_009_003_001, "会话不存在"),
    AGENT_SESSION_FORBIDDEN(1_009_003_002, "无权操作该会话"),
    AGENT_SESSION_INVALID(1_009_003_003, "会话参数无效"),
    AGENT_RUNTIME_FAILED(1_009_003_004, "智能体运行失败"),
    AGENT_MESSAGE_EMPTY(1_009_003_005, "消息内容不能为空"),

    AGENT_AUTOMATION_NOT_FOUND(1_009_004_001, "自动化不存在"),
    AGENT_AUTOMATION_FORBIDDEN(1_009_004_002, "无权操作该自动化"),
    AGENT_AUTOMATION_INVALID(1_009_004_003, "自动化参数无效"),
    AGENT_AUTOMATION_DISABLED(1_009_004_004, "自动化已停用"),
    AGENT_AUTOMATION_NO_CARRIER(1_009_004_005, "智能体尚未绑定 Carrier Bot");

    private final Integer code;
    private final String msg;
}
