package com.relayflow.module.calendar.service.notify;

import com.relayflow.module.calendar.dal.dataobject.CalEventDO;
import com.relayflow.module.calendar.enums.CalendarEventStatus;
import com.relayflow.module.im.api.bot.ImBotApi;
import com.relayflow.module.im.api.bot.dto.ImBotSendCommand;
import com.relayflow.module.im.api.bot.dto.ImBotSendTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

/**
 * Calendar reach via {@code calendar-bot} + {@link ImBotApi} (best-effort).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarBotNotifyService {

    private static final String BOT_CODE = "calendar-bot";
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ImBotApi imBotApi;

    public void notifyInvite(CalEventDO event, Collection<Long> userIds) {
        if (event == null || userIds == null || userIds.isEmpty()) {
            return;
        }
        String title = eventTitle(event);
        String text = "你被邀请参加日程「" + title + "」"
                + (event.getStartTime() != null ? "（" + event.getStartTime().format(TIME_FORMAT) + "）" : "");
        for (Long userId : userIds) {
            if (userId == null || userId.equals(event.getOrganizerId())) {
                continue;
            }
            // userId already scoped by ImBotApi client_msg_id; keep key short for VARCHAR(64).
            send(event, userId, text, "ci:" + event.getId());
        }
    }

    public void notifyUpdate(CalEventDO event, Collection<Long> userIds) {
        if (event == null || userIds == null || userIds.isEmpty()) {
            return;
        }
        String title = eventTitle(event);
        String text = "日程「" + title + "」已更新";
        String suffix = String.valueOf(System.currentTimeMillis() / 60_000);
        for (Long userId : userIds) {
            if (userId == null || userId.equals(event.getOrganizerId())) {
                continue;
            }
            send(event, userId, text, "cu:" + event.getId() + ":" + suffix);
        }
    }

    public void notifyCancel(CalEventDO event, Collection<Long> userIds) {
        if (event == null || userIds == null || userIds.isEmpty()) {
            return;
        }
        String title = eventTitle(event);
        String text = "日程「" + title + "」已取消";
        for (Long userId : userIds) {
            if (userId == null || userId.equals(event.getOrganizerId())) {
                continue;
            }
            send(event, userId, text, "cc:" + event.getId());
        }
    }

    public void pushRemindIfDue(CalEventDO event, Long userId) {
        if (!shouldRemind(event, userId)) {
            return;
        }
        String title = eventTitle(event);
        String text = "日程「" + title + "」即将开始"
                + (event.getStartTime() != null ? "（" + event.getStartTime().format(TIME_FORMAT) + "）" : "");
        send(event, userId, text, "cr:" + event.getId());
    }

    boolean shouldRemind(CalEventDO event, Long userId) {
        if (event == null || userId == null || event.getId() == null || event.getTenantId() == null) {
            return false;
        }
        if (!CalendarEventStatus.CONFIRMED.name().equals(event.getStatus())) {
            return false;
        }
        if (event.getStartTime() == null) {
            return false;
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (!event.getStartTime().isAfter(now)) {
            return false;
        }
        if (Boolean.TRUE.equals(toBool(event.getAllDay()))) {
            // All-day: remind on calendar day at configured clock is complex; V1 skip timed push for all-day
            // unless start is within next 24h (simple compensate).
            return !event.getStartTime().isAfter(now.plusHours(24));
        }
        Integer minutes = event.getRemindBeforeMinutes();
        if (minutes == null || minutes < 0) {
            return false;
        }
        OffsetDateTime remindAt = event.getStartTime().minusMinutes(minutes);
        return !now.isBefore(remindAt) && now.isBefore(event.getStartTime());
    }

    private void send(CalEventDO event, Long userId, String text, String dedupeKey) {
        ImBotSendTarget target = new ImBotSendTarget();
        target.setScope(ImBotSendTarget.SCOPE_SINGLE);
        target.setTenantId(event.getTenantId());
        target.setUserId(userId);

        ImBotSendCommand command = new ImBotSendCommand();
        command.setBotCode(BOT_CODE);
        command.setText(text);
        command.setDedupeKey(dedupeKey);
        command.setRoute("/app/calendar?eventId=" + event.getId());
        command.setEntityType("calendar_event");
        command.setEntityId(String.valueOf(event.getId()));
        command.setTarget(target);

        try {
            imBotApi.send(command);
        } catch (Exception ex) {
            log.warn("Calendar bot message failed: eventId={}, userId={}, dedupeKey={}",
                    event.getId(), userId, dedupeKey, ex);
        }
    }

    private static String eventTitle(CalEventDO event) {
        return StringUtils.hasText(event.getTitle()) ? event.getTitle() : "(无主题)";
    }

    private static Boolean toBool(Integer value) {
        return value != null && value != 0;
    }
}
