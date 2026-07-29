package com.relayflow.module.agent.controller.app.session.vo;

import lombok.Data;

/**
 * Message part aligned with workspace-agent-chat contract (text / reasoning / tool).
 */
@Data
public class AgentMessagePartVO {

    /** text | reasoning | tool */
    private String type;
    private String text;
    private String toolCallId;
    private String toolName;
    private String input;
    private String output;
}
