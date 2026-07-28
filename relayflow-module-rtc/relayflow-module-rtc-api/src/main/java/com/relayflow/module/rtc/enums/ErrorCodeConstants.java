package com.relayflow.module.rtc.enums;

import com.relayflow.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCodeConstants implements ErrorCode {

    RTC_SESSION_NOT_FOUND(1_007_001_001, "媒体会话不存在"),
    RTC_SESSION_ENDED(1_007_001_002, "媒体会话已结束"),
    RTC_LIVEKIT_NOT_CONFIGURED(1_007_001_003, "LiveKit 未配置"),
    RTC_WEBHOOK_UNAUTHORIZED(1_007_001_004, "LiveKit webhook 鉴权失败"),
    RTC_PURPOSE_INVALID(1_007_001_005, "无效的媒体会话用途");

    private final Integer code;
    private final String msg;
}
