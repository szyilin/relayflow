package com.relayflow.module.task.enums;

import com.relayflow.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCodeConstants implements ErrorCode {

    TASK_NOT_FOUND(1_004_001_001, "任务不存在"),
    TASK_FORBIDDEN(1_004_001_002, "无权操作该任务"),
    SEARCH_KEYWORD_REQUIRED(1_004_001_003, "请输入搜索关键词"),
    TASK_INVALID_TIME_RANGE(1_004_001_004, "开始时间不能晚于截止时间"),
    TASK_SUBTASK_DEPTH_EXCEEDED(1_004_001_005, "子任务不能再添加子任务"),
    TASK_ASSIGNEE_NOT_MEMBER(1_004_001_006, "指派对象非本租户有效成员"),
    TASK_COMMENT_EMPTY(1_004_001_007, "评论内容为空"),
    TASK_GROUP_MOVE_INVALID(1_004_001_008, "分组移动字段或值非法"),

    TASK_LIST_NOT_FOUND(1_004_002_001, "清单不存在"),
    TASK_LIST_FORBIDDEN(1_004_002_002, "无权操作该清单"),
    TASK_LIST_NAME_EMPTY(1_004_002_003, "清单名称不能为空"),
    TASK_LIST_MEMBER_NOT_TENANT(1_004_002_004, "邀请对象非本租户有效成员"),
    TASK_LIST_OWNER_REQUIRED(1_004_002_005, "清单须保留至少一名所有者"),

    TASK_VIEW_CONFIG_FORBIDDEN(1_004_003_001, "无权保存该视图配置"),

    TASK_MINE_GROUP_NOT_FOUND(1_004_004_001, "个人分组不存在"),
    TASK_MINE_GROUP_FORBIDDEN(1_004_004_002, "无权操作该个人分组"),
    TASK_MINE_GROUP_NAME_EMPTY(1_004_004_003, "分组名称不能为空"),

    TASK_LIST_GROUP_NOT_FOUND(1_004_005_001, "清单分组不存在"),
    TASK_LIST_GROUP_FORBIDDEN(1_004_005_002, "无权操作该清单分组"),
    TASK_LIST_GROUP_NAME_EMPTY(1_004_005_003, "分组名称不能为空"),

    TASK_LIST_FIELD_NOT_FOUND(1_004_006_001, "清单自定义字段不存在"),
    TASK_LIST_FIELD_OPTION_NOT_FOUND(1_004_006_002, "自定义字段选项不存在"),
    TASK_LIST_FIELD_FORBIDDEN(1_004_006_003, "无权操作该清单自定义字段"),
    TASK_LIST_FIELD_NAME_EMPTY(1_004_006_004, "字段名称不能为空"),
    TASK_LIST_FIELD_OPTIONS_MIN(1_004_006_005, "单选字段至少两个选项");

    private final Integer code;
    private final String msg;
}
