package com.relayflow.module.meeting.service.meeting;

import com.relayflow.module.meeting.controller.app.vo.MeetingInstantReqVO;
import com.relayflow.module.meeting.controller.app.vo.MeetingJoinCredentialsReqVO;
import com.relayflow.module.meeting.controller.app.vo.MeetingJoinCredentialsRespVO;
import com.relayflow.module.meeting.controller.app.vo.MeetingScheduleReqVO;
import com.relayflow.module.meeting.controller.app.vo.MeetingSummaryRespVO;

import java.util.List;

public interface MeetingService {

    MeetingSummaryRespVO createInstant(MeetingInstantReqVO request);

    MeetingSummaryRespVO schedule(MeetingScheduleReqVO request);

    MeetingSummaryRespVO getById(Long id);

    MeetingSummaryRespVO getByCode(String code);

    MeetingJoinCredentialsRespVO joinCredentials(MeetingJoinCredentialsReqVO request);

    void leave(Long meetingId);

    void end(Long meetingId);

    List<MeetingSummaryRespVO> history();
}
