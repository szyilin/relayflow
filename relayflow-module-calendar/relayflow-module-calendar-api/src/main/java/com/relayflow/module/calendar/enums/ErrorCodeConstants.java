package com.relayflow.module.calendar.enums;

import com.relayflow.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCodeConstants implements ErrorCode {

    CALENDAR_NOT_FOUND(1_005_001_001, "日历不存在"),
    CALENDAR_FORBIDDEN(1_005_001_002, "无权操作该日历"),
    PRIMARY_CALENDAR_DELETE_FORBIDDEN(1_005_001_003, "主日历不可删除"),
    CALENDAR_NOT_EMPTY(1_005_001_004, "日历下仍有日程，无法删除"),
    EVENT_NOT_FOUND(1_005_002_001, "日程不存在"),
    EVENT_FORBIDDEN(1_005_002_002, "无权操作该日程"),
    EVENT_TIME_INVALID(1_005_002_003, "日程时间无效"),
    ATTENDEE_INVALID(1_005_003_001, "参与人无效");

    private final Integer code;
    private final String msg;
}
