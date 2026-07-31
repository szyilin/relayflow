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
    SEARCH_KEYWORD_REQUIRED(1_003_003_005, "请输入搜索关键词"),
    GROUP_OWNER_REQUIRED(1_003_003_006, "仅群主可管理群内机器人"),
    GROUP_MEMBER_LIMIT_EXCEEDED(1_003_003_007, "超过群成员人数上限"),
    BOT_NOT_FOUND(1_003_004_001, "机器人不存在或已停用"),
    BOT_NOT_ENABLED_FOR_TENANT(1_003_004_002, "机器人未在该企业启用"),
    BOT_NOT_ENABLED_FOR_USER(1_003_004_003, "用户未启用该机器人"),
    BOT_SEND_INVALID(1_003_004_004, "机器人发送参数无效"),
    BOT_PROVISION_INVALID(1_003_004_005, "机器人供给参数无效"),
    BOT_NOT_EMBEDDABLE(1_003_004_006, "该机器人不支持加入群聊"),
    BOT_OWNER_REQUIRED(1_003_004_007, "仅智能体主人可将该机器人加入群聊"),
    CARD_CONTENT_INVALID(1_003_005_001, "卡片内容无效"),
    CARD_ACTION_INVALID(1_003_005_002, "卡片操作无效"),
    CARD_ACTION_EXPIRED(1_003_005_003, "卡片已过期"),
    CARD_ACTION_HANDLER_NOT_FOUND(1_003_005_004, "卡片处理器未注册"),
    PRODUCT_SETTINGS_INVALID(1_003_006_001, "产品设置参数无效"),
    GROUP_PUBLIC_DISABLED(1_003_003_008, "组织已禁止公开群"),
    GROUP_NOT_PUBLIC(1_003_003_009, "该群未公开，无法加入"),
    GROUP_LEAVE_DENIED(1_003_003_010, "部门群不允许退出"),
    GROUP_OWNER_LEAVE_DENIED(1_003_003_011, "请先转让群主"),
    GROUP_EXPORT_DENIED(1_003_003_012, "无权导出群成员"),
    MESSAGE_RECALL_DENIED(1_003_002_006, "无权撤回该消息"),
    MESSAGE_EDIT_DENIED(1_003_002_007, "无权编辑该消息"),
    MESSAGE_NOT_FOUND(1_003_002_008, "消息不存在"),
    MESSAGE_REPLY_INVALID(1_003_002_009, "引用消息无效或不在同一会话"),
    MESSAGE_READ_DENIED(1_003_002_010, "无权读取该会话消息"),
    MESSAGE_REACTION_INVALID(1_003_002_011, "表情回应无效"),
    MESSAGE_REACTION_DENIED(1_003_002_012, "无法回应此消息"),
    MESSAGE_FORWARD_DENIED(1_003_002_013, "消息不可转发"),
    STICKER_NOT_FOUND(1_003_002_014, "表情不存在"),
    STICKER_FILE_TYPE_INVALID(1_003_002_015, "自定义表情仅支持图片或动图"),
    STICKER_LIMIT_EXCEEDED(1_003_002_016, "自定义表情数量已达上限"),
    STICKER_COLLECT_DENIED(1_003_002_017, "无法添加到表情"),
    TAG_NAME_INVALID(1_003_007_001, "标签名称无效"),
    TAG_NOT_FOUND(1_003_007_002, "标签不存在");

    private final Integer code;
    private final String msg;
}
