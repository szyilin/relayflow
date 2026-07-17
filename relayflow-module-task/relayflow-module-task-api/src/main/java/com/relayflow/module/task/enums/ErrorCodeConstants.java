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

    TASK_LIST_NOT_FOUND(1_004_002_001, "清单不存在"),
    TASK_LIST_FORBIDDEN(1_004_002_002, "无权操作该清单"),
    TASK_LIST_NAME_EMPTY(1_004_002_003, "清单名称不能为空"),
    TASK_LIST_MEMBER_NOT_TENANT(1_004_002_004, "邀请对象非本租户有效成员"),
    TASK_LIST_OWNER_REQUIRED(1_004_002_005, "清单须保留至少一名所有者");

    private final Integer code;
    private final String msg;
}
