package com.relayflow.module.agent.service.automation;

import com.relayflow.module.agent.controller.app.automation.vo.AgentAutomationRespVO;
import com.relayflow.module.agent.controller.app.automation.vo.AgentAutomationSaveReqVO;

import java.util.List;

public interface AgentAutomationService {

    List<AgentAutomationRespVO> listMine();

    AgentAutomationRespVO getMine(Long id);

    Long create(AgentAutomationSaveReqVO request);

    void update(AgentAutomationSaveReqVO request);

    void delete(Long id);

    /** Manual trigger for owner. */
    void runNow(Long id);

    /** Scheduler entry: execute all due automations. */
    int runDueAutomations();
}
