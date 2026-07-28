package com.relayflow.module.meeting.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.meeting.controller.app.vo.MeetingIdReqVO;
import com.relayflow.module.meeting.controller.app.vo.MeetingInstantReqVO;
import com.relayflow.module.meeting.controller.app.vo.MeetingJoinCredentialsReqVO;
import com.relayflow.module.meeting.controller.app.vo.MeetingJoinCredentialsRespVO;
import com.relayflow.module.meeting.controller.app.vo.MeetingScheduleReqVO;
import com.relayflow.module.meeting.controller.app.vo.MeetingSummaryRespVO;
import com.relayflow.module.meeting.service.meeting.MeetingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.relayflow.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/app-api/meeting")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping("/instant")
    public CommonResult<MeetingSummaryRespVO> instant(@RequestBody(required = false) MeetingInstantReqVO request) {
        return success(meetingService.createInstant(request != null ? request : new MeetingInstantReqVO()));
    }

    @PostMapping("/schedule")
    public CommonResult<MeetingSummaryRespVO> schedule(@Valid @RequestBody MeetingScheduleReqVO request) {
        return success(meetingService.schedule(request));
    }

    @GetMapping("/get")
    public CommonResult<MeetingSummaryRespVO> get(@RequestParam("id") Long id) {
        return success(meetingService.getById(id));
    }

    @GetMapping("/get-by-code")
    public CommonResult<MeetingSummaryRespVO> getByCode(@RequestParam("code") String code) {
        return success(meetingService.getByCode(code));
    }

    @PostMapping("/join-credentials")
    public CommonResult<MeetingJoinCredentialsRespVO> joinCredentials(
            @RequestBody MeetingJoinCredentialsReqVO request) {
        return success(meetingService.joinCredentials(request));
    }

    @PostMapping("/leave")
    public CommonResult<Boolean> leave(@Valid @RequestBody MeetingIdReqVO request) {
        meetingService.leave(request.getMeetingId());
        return success(true);
    }

    @PostMapping("/end")
    public CommonResult<Boolean> end(@Valid @RequestBody MeetingIdReqVO request) {
        meetingService.end(request.getMeetingId());
        return success(true);
    }

    @GetMapping("/history")
    public CommonResult<List<MeetingSummaryRespVO>> history() {
        return success(meetingService.history());
    }
}
