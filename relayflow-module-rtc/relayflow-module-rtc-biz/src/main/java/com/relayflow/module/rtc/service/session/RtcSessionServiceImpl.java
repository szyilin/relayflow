package com.relayflow.module.rtc.service.session;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.rtc.api.session.dto.RtcJoinTokenReqDTO;
import com.relayflow.module.rtc.api.session.dto.RtcJoinTokenRespDTO;
import com.relayflow.module.rtc.api.session.dto.RtcSessionCreateReqDTO;
import com.relayflow.module.rtc.api.session.dto.RtcSessionRespDTO;
import com.relayflow.module.rtc.config.RtcLiveKitProperties;
import com.relayflow.module.rtc.dal.dataobject.RtcSessionDO;
import com.relayflow.module.rtc.dal.mapper.RtcSessionMapper;
import com.relayflow.module.rtc.enums.ErrorCodeConstants;
import com.relayflow.module.rtc.enums.RtcPurpose;
import io.livekit.server.AccessToken;
import io.livekit.server.CanPublish;
import io.livekit.server.CanSubscribe;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RtcSessionServiceImpl implements RtcSessionService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_ENDED = "ENDED";

    private final RtcSessionMapper rtcSessionMapper;
    private final RtcLiveKitProperties liveKitProperties;

    @Override
    @Transactional
    public RtcSessionRespDTO ensureSession(RtcSessionCreateReqDTO request) {
        requireConfigured();
        String purpose = normalizePurpose(request.getPurpose());
        if (!StringUtils.hasText(request.getOwnerType()) || request.getOwnerId() == null) {
            throw new ServiceException(ErrorCodeConstants.RTC_PURPOSE_INVALID.getCode(), "ownerType/ownerId 必填");
        }

        RtcSessionDO existing = rtcSessionMapper.selectOne(new LambdaQueryWrapper<RtcSessionDO>()
                .eq(RtcSessionDO::getOwnerType, request.getOwnerType())
                .eq(RtcSessionDO::getOwnerId, request.getOwnerId())
                .eq(RtcSessionDO::getPurpose, purpose)
                .eq(RtcSessionDO::getStatus, STATUS_ACTIVE)
                .last("LIMIT 1"));
        if (existing != null) {
            return toResp(existing);
        }

        RtcSessionDO row = new RtcSessionDO();
        long id = IdWorker.getId();
        row.setId(id);
        row.setPurpose(purpose);
        row.setOwnerType(request.getOwnerType().trim());
        row.setOwnerId(request.getOwnerId());
        row.setRoomName("rf_" + id);
        row.setStatus(STATUS_ACTIVE);
        row.setParticipantCount(0);
        rtcSessionMapper.insert(row);
        return toResp(row);
    }

    @Override
    public RtcSessionRespDTO getSession(Long sessionId) {
        RtcSessionDO row = requireSession(sessionId);
        return toResp(row);
    }

    @Override
    public RtcJoinTokenRespDTO issueJoinToken(RtcJoinTokenReqDTO request) {
        requireConfigured();
        if (request.getUserId() == null) {
            throw new ServiceException(ErrorCodeConstants.RTC_SESSION_NOT_FOUND.getCode(), "userId 必填");
        }
        RtcSessionDO session = requireSession(request.getSessionId());
        if (STATUS_ENDED.equals(session.getStatus())) {
            throw new ServiceException(ErrorCodeConstants.RTC_SESSION_ENDED);
        }

        long ttl = request.getTtlSeconds() != null && request.getTtlSeconds() > 0
                ? request.getTtlSeconds()
                : liveKitProperties.getTokenTtlSeconds();
        boolean canPublish = request.getCanPublish() == null || request.getCanPublish();
        boolean canSubscribe = request.getCanSubscribe() == null || request.getCanSubscribe();
        String identity = String.valueOf(request.getUserId());
        String name = StringUtils.hasText(request.getDisplayName()) ? request.getDisplayName() : identity;

        AccessToken token = new AccessToken(liveKitProperties.getApiKey(), liveKitProperties.getApiSecret());
        token.setIdentity(identity);
        token.setName(name);
        token.setTtl(ttl * 1000L);
        token.addGrants(
                new RoomJoin(true),
                new RoomName(session.getRoomName()),
                new CanPublish(canPublish),
                new CanSubscribe(canSubscribe)
        );

        long expiresAt = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) + ttl;
        RtcJoinTokenRespDTO resp = new RtcJoinTokenRespDTO();
        resp.setSessionId(session.getId());
        resp.setRoomName(session.getRoomName());
        resp.setLivekitUrl(liveKitProperties.getUrl());
        resp.setToken(token.toJwt());
        resp.setIdentity(identity);
        resp.setExpiresAtEpochSeconds(expiresAt);
        return resp;
    }

    @Override
    @Transactional
    public void endSession(Long sessionId) {
        RtcSessionDO session = requireSession(sessionId);
        if (STATUS_ENDED.equals(session.getStatus())) {
            return;
        }
        session.setStatus(STATUS_ENDED);
        rtcSessionMapper.updateById(session);
    }

    @Override
    public RtcSessionDO findByRoomName(String roomName) {
        if (!StringUtils.hasText(roomName)) {
            return null;
        }
        return rtcSessionMapper.selectOne(new LambdaQueryWrapper<RtcSessionDO>()
                .eq(RtcSessionDO::getRoomName, roomName)
                .last("LIMIT 1"));
    }

    @Override
    @Transactional
    public void applyParticipantDelta(String roomName, int delta) {
        RtcSessionDO session = findByRoomName(roomName);
        if (session == null || STATUS_ENDED.equals(session.getStatus())) {
            return;
        }
        int next = Math.max(0, (session.getParticipantCount() == null ? 0 : session.getParticipantCount()) + delta);
        session.setParticipantCount(next);
        rtcSessionMapper.updateById(session);
    }

    @Override
    @Transactional
    public void markEndedByRoomName(String roomName) {
        RtcSessionDO session = findByRoomName(roomName);
        if (session == null || STATUS_ENDED.equals(session.getStatus())) {
            return;
        }
        session.setStatus(STATUS_ENDED);
        rtcSessionMapper.updateById(session);
    }

    private void requireConfigured() {
        if (!liveKitProperties.isConfigured()) {
            throw new ServiceException(ErrorCodeConstants.RTC_LIVEKIT_NOT_CONFIGURED);
        }
    }

    private RtcSessionDO requireSession(Long sessionId) {
        if (sessionId == null) {
            throw new ServiceException(ErrorCodeConstants.RTC_SESSION_NOT_FOUND);
        }
        RtcSessionDO row = rtcSessionMapper.selectById(sessionId);
        if (row == null) {
            throw new ServiceException(ErrorCodeConstants.RTC_SESSION_NOT_FOUND);
        }
        return row;
    }

    private static String normalizePurpose(String purpose) {
        if (!StringUtils.hasText(purpose)) {
            throw new ServiceException(ErrorCodeConstants.RTC_PURPOSE_INVALID);
        }
        try {
            return RtcPurpose.valueOf(purpose.trim().toUpperCase()).name();
        } catch (IllegalArgumentException ex) {
            throw new ServiceException(ErrorCodeConstants.RTC_PURPOSE_INVALID);
        }
    }

    private static RtcSessionRespDTO toResp(RtcSessionDO row) {
        RtcSessionRespDTO dto = new RtcSessionRespDTO();
        dto.setId(row.getId());
        dto.setPurpose(row.getPurpose());
        dto.setOwnerType(row.getOwnerType());
        dto.setOwnerId(row.getOwnerId());
        dto.setRoomName(row.getRoomName());
        dto.setStatus(row.getStatus());
        dto.setParticipantCount(row.getParticipantCount());
        return dto;
    }
}
