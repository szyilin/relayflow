package com.relayflow.module.agent.service.automation;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutomationCronSupportTest {

    @Test
    void nextRunAfter_everyMinute() {
        OffsetDateTime from = OffsetDateTime.of(2026, 7, 28, 10, 0, 0, 0, ZoneOffset.ofHours(8));
        OffsetDateTime next = AutomationCronSupport.nextRunAfter("0 * * * * ?", from);
        assertNotNull(next);
        assertTrue(next.isAfter(from));
    }

    @Test
    void parse_rejectsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> AutomationCronSupport.parse("not-a-cron"));
    }

    @Test
    void carrierBotCode_stable() {
        assertTrue(AgentAutomationServiceImpl.carrierBotCode(99L).equals("agent-carrier-99"));
    }
}
