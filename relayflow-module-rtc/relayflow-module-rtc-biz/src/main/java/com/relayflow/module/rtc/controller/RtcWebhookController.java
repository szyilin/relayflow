package com.relayflow.module.rtc.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.rtc.config.RtcLiveKitProperties;
import com.relayflow.module.rtc.enums.ErrorCodeConstants;
import com.relayflow.module.rtc.service.session.RtcSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.relayflow.common.pojo.CommonResult.success;

/**
 * LiveKit webhook — no user JWT; verified via shared secret Bearer token.
 * Updates rtc_* only; never writes mtg_*.
 * Wire LiveKit to POST /app-api/rtc/webhook (see docs/dev/rtc-livekit.md).
 * Default Compose --dev does not deliver webhooks until configured;
 * containers may need host.docker.internal or a host LAN IP to reach Java.
 */
@Slf4j
@RestController
@RequestMapping("/app-api/rtc")
@RequiredArgsConstructor
public class RtcWebhookController {

    private final RtcLiveKitProperties liveKitProperties;
    private final RtcSessionService rtcSessionService;
    private final ObjectMapper objectMapper;

    @PostMapping("/webhook")
    public CommonResult<Boolean> webhook(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody String body
    ) {
        verifyAuth(authorization);
        try {
            JsonNode root = objectMapper.readTree(body);
            String event = text(root, "event");
            JsonNode room = root.path("room");
            String roomName = text(room, "name");
            if (!StringUtils.hasText(roomName)) {
                return success(true);
            }
            switch (event == null ? "" : event) {
                case "participant_joined" -> rtcSessionService.applyParticipantDelta(roomName, 1);
                case "participant_left" -> rtcSessionService.applyParticipantDelta(roomName, -1);
                case "room_finished" -> rtcSessionService.markEndedByRoomName(roomName);
                default -> log.debug("Ignored LiveKit webhook event={}", event);
            }
        } catch (Exception ex) {
            log.warn("LiveKit webhook parse/handle failed: {}", ex.toString());
        }
        return success(true);
    }

    private void verifyAuth(String authorization) {
        String expected = liveKitProperties.resolveWebhookSecret();
        if (!StringUtils.hasText(expected)) {
            throw new ServiceException(ErrorCodeConstants.RTC_WEBHOOK_UNAUTHORIZED);
        }
        if (!StringUtils.hasText(authorization)) {
            throw new ServiceException(ErrorCodeConstants.RTC_WEBHOOK_UNAUTHORIZED);
        }
        String token = authorization.startsWith("Bearer ")
                ? authorization.substring("Bearer ".length()).trim()
                : authorization.trim();
        if (!expected.equals(token)) {
            throw new ServiceException(ErrorCodeConstants.RTC_WEBHOOK_UNAUTHORIZED);
        }
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }
}
