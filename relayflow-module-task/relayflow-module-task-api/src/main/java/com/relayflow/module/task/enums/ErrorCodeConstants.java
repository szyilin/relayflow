package com.relayflow.module.task.enums;

import com.relayflow.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCodeConstants implements ErrorCode {

    TASK_NOT_FOUND(1_004_001_001, "任务不存在"),
    TASK_FORBIDDEN(1_004_001_002, "无权操作该任务"),
    SEARCH_KEYWORD_REQUIRED(1_004_001_003, "请输入搜索关键词");

    private final Integer code;
    private final String msg;
}
