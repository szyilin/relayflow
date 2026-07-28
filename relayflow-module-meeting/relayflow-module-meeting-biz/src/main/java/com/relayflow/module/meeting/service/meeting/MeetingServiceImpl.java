package com.relayflow.module.meeting.service.meeting;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.calendar.api.event.CalEventApi;
import com.relayflow.module.meeting.controller.app.vo.MeetingInstantReqVO;
import com.relayflow.module.meeting.controller.app.vo.MeetingJoinCredentialsReqVO;
import com.relayflow.module.meeting.controller.app.vo.MeetingJoinCredentialsRespVO;
import com.relayflow.module.meeting.controller.app.vo.MeetingScheduleReqVO;
import com.relayflow.module.meeting.controller.app.vo.MeetingSummaryRespVO;
import com.relayflow.module.meeting.dal.dataobject.MtgMeetingDO;
import com.relayflow.module.meeting.dal.dataobject.MtgParticipantDO;
import com.relayflow.module.meeting.dal.mapper.MtgMeetingMapper;
import com.relayflow.module.meeting.dal.mapper.MtgParticipantMapper;
import com.relayflow.module.meeting.enums.ErrorCodeConstants;
import com.relayflow.module.meeting.enums.MeetingParticipantRole;
import com.relayflow.module.meeting.enums.MeetingStatus;
import com.relayflow.module.rtc.api.session.RtcSessionApi;
import com.relayflow.module.rtc.api.session.dto.RtcJoinTokenReqDTO;
import com.relayflow.module.rtc.api.session.dto.RtcJoinTokenRespDTO;
import com.relayflow.module.rtc.api.session.dto.RtcSessionCreateReqDTO;
import com.relayflow.module.rtc.api.session.dto.RtcSessionRespDTO;
import com.relayflow.module.rtc.enums.RtcPurpose;
import com.relayflow.module.system.api.tenant.TenantMemberApi;
import com.relayflow.module.system.api.user.UserApi;
import com.relayflow.module.system.api.user.dto.UserBasicDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingServiceImpl implements MeetingService {

    private static final String OWNER_TYPE_MEETING = "MEETING";
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final int CODE_RETRY = 8;
    private static final int HISTORY_LIMIT = 50;

    private final MtgMeetingMapper mtgMeetingMapper;
    private final MtgParticipantMapper mtgParticipantMapper;
    private final RtcSessionApi rtcSessionApi;
    private final CalEventApi calEventApi;
    private final TenantMemberApi tenantMemberApi;
    private final UserApi userApi;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public MeetingSummaryRespVO createInstant(MeetingInstantReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        requireActiveMember(tenantId, userId);

        OffsetDateTime now = OffsetDateTime.now();
        MtgMeetingDO meeting = newMeetingShell(tenantId, userId, normalizeTitle(request != null ? request.getTitle() : null, "即时会议"),
                MeetingStatus.LIVE.name(), null, null, now);
        mtgMeetingMapper.insert(meeting);
        upsertParticipant(meeting, userId, MeetingParticipantRole.HOST, now, false);
        return toSummary(meeting);
    }

    @Override
    @Transactional
    public MeetingSummaryRespVO schedule(MeetingScheduleReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        requireActiveMember(tenantId, userId);
        if (request.getScheduledStart() == null || request.getScheduledEnd() == null
                || !request.getScheduledEnd().isAfter(request.getScheduledStart())) {
            throw new ServiceException(ErrorCodeConstants.MEETING_TIME_INVALID);
        }

        OffsetDateTime now = OffsetDateTime.now();
        MtgMeetingDO meeting = newMeetingShell(tenantId, userId,
                normalizeTitle(request.getTitle(), "预约会议"),
                MeetingStatus.SCHEDULED.name(),
                request.getScheduledStart(), request.getScheduledEnd(), now);
        mtgMeetingMapper.insert(meeting);
        upsertParticipant(meeting, userId, MeetingParticipantRole.HOST, now, false);
        return toSummary(meeting);
    }

    @Override
    public MeetingSummaryRespVO getById(Long id) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        requireActiveMember(tenantId, userId);
        return toSummary(requireMeeting(id));
    }

    @Override
    public MeetingSummaryRespVO getByCode(String code) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        requireActiveMember(tenantId, userId);
        return toSummary(requireMeetingByCode(code));
    }

    @Override
    @Transactional
    public MeetingJoinCredentialsRespVO joinCredentials(MeetingJoinCredentialsReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        requireActiveMember(tenantId, userId);

        MtgMeetingDO meeting = resolveMeeting(request);
        if (MeetingStatus.ENDED.name().equals(meeting.getStatus())) {
            throw new ServiceException(ErrorCodeConstants.MEETING_ENDED);
        }

        if (meeting.getRtcSessionId() == null) {
            RtcSessionCreateReqDTO createReq = new RtcSessionCreateReqDTO();
            createReq.setPurpose(RtcPurpose.MEETING.name());
            createReq.setOwnerType(OWNER_TYPE_MEETING);
            createReq.setOwnerId(meeting.getId());
            RtcSessionRespDTO session = rtcSessionApi.ensureSession(createReq);
            meeting.setRtcSessionId(session.getId());
        }

        if (MeetingStatus.SCHEDULED.name().equals(meeting.getStatus())) {
            meeting.setStatus(MeetingStatus.LIVE.name());
        }
        meeting.setUpdateTime(OffsetDateTime.now());
        mtgMeetingMapper.updateById(meeting);

        OffsetDateTime now = OffsetDateTime.now();
        MeetingParticipantRole role = meeting.getHostUserId().equals(userId)
                ? MeetingParticipantRole.HOST
                : MeetingParticipantRole.ATTENDEE;
        upsertParticipant(meeting, userId, role, now, true);

        RtcJoinTokenReqDTO tokenReq = new RtcJoinTokenReqDTO();
        tokenReq.setSessionId(meeting.getRtcSessionId());
        tokenReq.setUserId(userId);
        tokenReq.setDisplayName(resolveDisplayName(userId));
        RtcJoinTokenRespDTO token = rtcSessionApi.issueJoinToken(tokenReq);

        MeetingJoinCredentialsRespVO resp = new MeetingJoinCredentialsRespVO();
        resp.setMeetingId(meeting.getId());
        resp.setMeetingCode(meeting.getMeetingCode());
        resp.setTitle(meeting.getTitle());
        resp.setLivekitUrl(token.getLivekitUrl());
        resp.setToken(token.getToken());
        resp.setRoomName(token.getRoomName());
        resp.setIdentity(token.getIdentity());
        return resp;
    }

    @Override
    @Transactional
    public void leave(Long meetingId) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        requireActiveMember(tenantId, userId);
        MtgMeetingDO meeting = requireMeeting(meetingId);
        MtgParticipantDO row = mtgParticipantMapper.selectOne(new LambdaQueryWrapper<MtgParticipantDO>()
                .eq(MtgParticipantDO::getMeetingId, meeting.getId())
                .eq(MtgParticipantDO::getUserId, userId)
                .last("LIMIT 1"));
        if (row == null) {
            return;
        }
        row.setLeftAt(OffsetDateTime.now());
        row.setUpdateTime(OffsetDateTime.now());
        mtgParticipantMapper.updateById(row);

        Long stillIn = mtgParticipantMapper.selectCount(new LambdaQueryWrapper<MtgParticipantDO>()
                .eq(MtgParticipantDO::getMeetingId, meeting.getId())
                .isNotNull(MtgParticipantDO::getJoinedAt)
                .isNull(MtgParticipantDO::getLeftAt));
        if (stillIn == null || stillIn == 0) {
            // Also end if nobody ever joined (e.g. host cancelled after create) and caller left.
            Long everJoined = mtgParticipantMapper.selectCount(new LambdaQueryWrapper<MtgParticipantDO>()
                    .eq(MtgParticipantDO::getMeetingId, meeting.getId())
                    .isNotNull(MtgParticipantDO::getJoinedAt));
            if (everJoined != null && everJoined > 0) {
                endMeetingInternal(meeting);
            }
        }
    }

    @Override
    @Transactional
    public void end(Long meetingId) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        requireActiveMember(tenantId, userId);
        MtgMeetingDO meeting = requireMeeting(meetingId);
        if (!meeting.getHostUserId().equals(userId)) {
            throw new ServiceException(ErrorCodeConstants.MEETING_HOST_REQUIRED);
        }
        endMeetingInternal(meeting);
    }

    private void endMeetingInternal(MtgMeetingDO meeting) {
        if (MeetingStatus.ENDED.name().equals(meeting.getStatus())) {
            return;
        }
        meeting.setStatus(MeetingStatus.ENDED.name());
        meeting.setUpdateTime(OffsetDateTime.now());
        mtgMeetingMapper.updateById(meeting);

        if (meeting.getRtcSessionId() != null) {
            try {
                rtcSessionApi.endSession(meeting.getRtcSessionId());
            } catch (RuntimeException ex) {
                log.warn("RtcSessionApi.endSession failed meetingId={}: {}", meeting.getId(), ex.toString());
            }
        }
        try {
            calEventApi.clearMeetingLink(meeting.getId());
        } catch (RuntimeException ex) {
            log.warn("CalEventApi.clearMeetingLink failed meetingId={}: {}", meeting.getId(), ex.toString());
        }
    }

    @Override
    public List<MeetingSummaryRespVO> history() {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        requireActiveMember(tenantId, userId);

        List<MtgParticipantDO> parts = mtgParticipantMapper.selectList(new LambdaQueryWrapper<MtgParticipantDO>()
                .eq(MtgParticipantDO::getUserId, userId)
                .orderByDesc(MtgParticipantDO::getUpdateTime)
                .last("LIMIT " + HISTORY_LIMIT));
        Set<Long> meetingIds = new HashSet<>();
        for (MtgParticipantDO p : parts) {
            meetingIds.add(p.getMeetingId());
        }
        List<MtgMeetingDO> hosted = mtgMeetingMapper.selectList(new LambdaQueryWrapper<MtgMeetingDO>()
                .eq(MtgMeetingDO::getHostUserId, userId)
                .orderByDesc(MtgMeetingDO::getCreateTime)
                .last("LIMIT " + HISTORY_LIMIT));
        for (MtgMeetingDO m : hosted) {
            meetingIds.add(m.getId());
        }
        if (meetingIds.isEmpty()) {
            return List.of();
        }
        List<MtgMeetingDO> meetings = mtgMeetingMapper.selectList(new LambdaQueryWrapper<MtgMeetingDO>()
                .in(MtgMeetingDO::getId, meetingIds));
        meetings.sort(Comparator.comparing(MtgMeetingDO::getCreateTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        List<MeetingSummaryRespVO> result = new ArrayList<>(meetings.size());
        for (MtgMeetingDO m : meetings) {
            result.add(toSummary(m));
            if (result.size() >= HISTORY_LIMIT) {
                break;
            }
        }
        return result;
    }

    private MtgMeetingDO newMeetingShell(Long tenantId, Long userId, String title, String status,
                                         OffsetDateTime scheduledStart, OffsetDateTime scheduledEnd,
                                         OffsetDateTime now) {
        MtgMeetingDO meeting = new MtgMeetingDO();
        meeting.setId(IdWorker.getId());
        meeting.setTenantId(tenantId);
        meeting.setMeetingCode(allocateMeetingCode(tenantId));
        meeting.setTitle(title);
        meeting.setHostUserId(userId);
        meeting.setStatus(status);
        meeting.setScheduledStart(scheduledStart);
        meeting.setScheduledEnd(scheduledEnd);
        meeting.setCreator(userId);
        meeting.setCreateTime(now);
        meeting.setUpdater(userId);
        meeting.setUpdateTime(now);
        return meeting;
    }

    private String allocateMeetingCode(Long tenantId) {
        for (int i = 0; i < CODE_RETRY; i++) {
            String code = randomCode();
            Long count = mtgMeetingMapper.selectCount(new LambdaQueryWrapper<MtgMeetingDO>()
                    .eq(MtgMeetingDO::getTenantId, tenantId)
                    .eq(MtgMeetingDO::getMeetingCode, code));
            if (count == null || count == 0) {
                return code;
            }
        }
        throw new ServiceException(ErrorCodeConstants.MEETING_CODE_INVALID.getCode(), "会议号生成失败，请重试");
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_ALPHABET.charAt(secureRandom.nextInt(CODE_ALPHABET.length())));
        }
        return sb.toString();
    }

    private void upsertParticipant(MtgMeetingDO meeting, Long userId, MeetingParticipantRole role,
                                   OffsetDateTime now, boolean markJoined) {
        MtgParticipantDO existing = mtgParticipantMapper.selectOne(new LambdaQueryWrapper<MtgParticipantDO>()
                .eq(MtgParticipantDO::getMeetingId, meeting.getId())
                .eq(MtgParticipantDO::getUserId, userId)
                .last("LIMIT 1"));
        if (existing == null) {
            MtgParticipantDO row = new MtgParticipantDO();
            row.setId(IdWorker.getId());
            row.setTenantId(meeting.getTenantId());
            row.setMeetingId(meeting.getId());
            row.setUserId(userId);
            row.setRole(role.name());
            if (markJoined) {
                row.setJoinedAt(now);
            }
            row.setLeftAt(null);
            row.setCreator(userId);
            row.setCreateTime(now);
            row.setUpdater(userId);
            row.setUpdateTime(now);
            mtgParticipantMapper.insert(row);
            return;
        }
        existing.setRole(role.name());
        if (markJoined) {
            existing.setJoinedAt(now);
            existing.setLeftAt(null);
        }
        existing.setUpdater(userId);
        existing.setUpdateTime(now);
        mtgParticipantMapper.updateById(existing);
    }

    private MtgMeetingDO resolveMeeting(MeetingJoinCredentialsReqVO request) {
        if (request == null) {
            throw new ServiceException(ErrorCodeConstants.MEETING_NOT_FOUND);
        }
        if (request.getMeetingId() != null) {
            return requireMeeting(request.getMeetingId());
        }
        if (StringUtils.hasText(request.getMeetingCode())) {
            return requireMeetingByCode(request.getMeetingCode());
        }
        throw new ServiceException(ErrorCodeConstants.MEETING_NOT_FOUND);
    }

    private MtgMeetingDO requireMeeting(Long id) {
        if (id == null) {
            throw new ServiceException(ErrorCodeConstants.MEETING_NOT_FOUND);
        }
        MtgMeetingDO meeting = mtgMeetingMapper.selectById(id);
        if (meeting == null) {
            throw new ServiceException(ErrorCodeConstants.MEETING_NOT_FOUND);
        }
        return meeting;
    }

    private MtgMeetingDO requireMeetingByCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new ServiceException(ErrorCodeConstants.MEETING_CODE_INVALID);
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        MtgMeetingDO meeting = mtgMeetingMapper.selectOne(new LambdaQueryWrapper<MtgMeetingDO>()
                .eq(MtgMeetingDO::getMeetingCode, normalized)
                .last("LIMIT 1"));
        if (meeting == null) {
            throw new ServiceException(ErrorCodeConstants.MEETING_NOT_FOUND);
        }
        return meeting;
    }

    private void requireActiveMember(Long tenantId, Long userId) {
        Set<Long> active = tenantMemberApi.filterActiveMemberUserIds(tenantId, List.of(userId));
        if (active == null || !active.contains(userId)) {
            throw new ServiceException(ErrorCodeConstants.MEETING_MEMBER_REQUIRED);
        }
    }

    private String resolveDisplayName(Long userId) {
        try {
            UserBasicDTO basic = userApi.getUserBasic(userId);
            if (basic != null && StringUtils.hasText(basic.getNickname())) {
                return basic.getNickname();
            }
            if (basic != null && StringUtils.hasText(basic.getUsername())) {
                return basic.getUsername();
            }
        } catch (Exception ex) {
            log.debug("Resolve display name failed userId={}", userId, ex);
        }
        return String.valueOf(userId);
    }

    private static String normalizeTitle(String title, String fallback) {
        if (!StringUtils.hasText(title)) {
            return fallback;
        }
        String trimmed = title.trim();
        return trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
    }

    private static MeetingSummaryRespVO toSummary(MtgMeetingDO meeting) {
        MeetingSummaryRespVO vo = new MeetingSummaryRespVO();
        vo.setId(meeting.getId());
        vo.setMeetingCode(meeting.getMeetingCode());
        vo.setTitle(meeting.getTitle());
        vo.setStatus(meeting.getStatus());
        vo.setHostUserId(meeting.getHostUserId());
        vo.setScheduledStart(meeting.getScheduledStart());
        vo.setScheduledEnd(meeting.getScheduledEnd());
        vo.setCalendarEventId(meeting.getCalendarEventId());
        vo.setCreateTime(meeting.getCreateTime());
        return vo;
    }
}
