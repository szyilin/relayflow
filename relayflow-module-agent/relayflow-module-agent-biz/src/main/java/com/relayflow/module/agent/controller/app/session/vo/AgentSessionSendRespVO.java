package com.relayflow.module.agent.controller.app.session.vo;

import lombok.Data;

@Data
public class AgentSessionSendRespVO {

    private AgentSessionMessageRespVO userMessage;
    private AgentSessionMessageRespVO assistantMessage;
}
