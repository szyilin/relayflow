package com.relayflow.module.meeting.enums;

import com.relayflow.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCodeConstants implements ErrorCode {

    MEETING_NOT_FOUND(1_008_001_001, "会议不存在"),
    MEETING_ENDED(1_008_001_002, "会议已结束"),
    MEETING_FORBIDDEN(1_008_001_003, "无权操作该会议"),
    MEETING_CODE_INVALID(1_008_001_004, "会议号无效"),
    MEETING_TIME_INVALID(1_008_001_005, "会议时间无效"),
    MEETING_HOST_REQUIRED(1_008_001_006, "仅主持人可结束会议"),
    MEETING_MEMBER_REQUIRED(1_008_001_007, "仅本企业有效成员可加入会议");

    private final Integer code;
    private final String msg;
}
