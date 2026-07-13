package com.relayflow.module.im.enums;

import com.relayflow.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCodeConstants implements ErrorCode {

    CONVERSATION_NOT_FOUND(1_003_001_001, "会话不存在"),
    CONVERSATION_ACCESS_DENIED(1_003_001_002, "无权访问该会话"),
    MESSAGE_SEND_INVALID(1_003_002_001, "消息参数无效"),
    PEER_USER_INVALID(1_003_002_002, "对方用户无效"),
    MESSAGE_CONTENT_INVALID(1_003_002_003, "消息内容格式无效"),
    MESSAGE_FILE_NOT_FOUND(1_003_002_004, "附件不存在或无权访问"),
    MESSAGE_FILE_TYPE_INVALID(1_003_002_005, "图片类型无效"),
    GROUP_NAME_INVALID(1_003_003_001, "群名称无效"),
    GROUP_MEMBER_REQUIRED(1_003_003_002, "请至少选择一名成员"),
    GROUP_NOT_FOUND(1_003_003_003, "群聊不存在"),
    GROUP_MEMBER_INVALID(1_003_003_004, "成员用户无效"),
    SEARCH_KEYWORD_REQUIRED(1_003_003_005, "请输入搜索关键词");

    private final Integer code;
    private final String msg;
}
