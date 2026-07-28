package com.relayflow.module.agent.job;

import com.relayflow.module.agent.service.automation.AgentAutomationService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Minute tick for due agent automations. Seed Cron: {@code 0 * * * * ?} in deploy/xxl-job/seed.
 */
@Component
@RequiredArgsConstructor
public class AgentAutomationTickJob {

    private final AgentAutomationService agentAutomationService;

    @XxlJob("agentAutomationTick")
    public void execute() {
        int ran = agentAutomationService.runDueAutomations();
        XxlJobHelper.log("agentAutomationTick finished, ran=" + ran);
        XxlJobHelper.handleSuccess("ran=" + ran);
    }
}
