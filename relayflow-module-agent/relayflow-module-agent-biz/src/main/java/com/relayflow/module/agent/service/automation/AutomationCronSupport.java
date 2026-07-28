package com.relayflow.module.agent.service.automation;

import com.relayflow.module.agent.dal.dataobject.AgentAutomationDO;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

final class AutomationCronSupport {

    private AutomationCronSupport() {
    }

    static CronExpression parse(String cronExpr) {
        if (!StringUtils.hasText(cronExpr)) {
            throw new IllegalArgumentException("cron empty");
        }
        return CronExpression.parse(cronExpr.trim());
    }

    static OffsetDateTime nextRunAfter(String cronExpr, OffsetDateTime from) {
        CronExpression cron = parse(cronExpr);
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime base = (from == null ? OffsetDateTime.now() : from).atZoneSameInstant(zone);
        ZonedDateTime next = cron.next(base);
        if (next == null) {
            return null;
        }
        return next.toOffsetDateTime();
    }

    static boolean isDue(AgentAutomationDO row, OffsetDateTime now) {
        if (row == null || row.getNextRunAt() == null) {
            return false;
        }
        return !row.getNextRunAt().isAfter(now);
    }

    static Instant nowInstant() {
        return Instant.now();
    }
}
