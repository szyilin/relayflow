package com.relayflow.module.agent.channel;

/**
 * Runtime channel that drives hot-path pack assembly.
 */
public enum AgentChannel {

    /** Workspace Agent Session. */
    AGENT_WEB("agent_web"),

    /** IM Carrier (bot_dm / group @). */
    IM_BOT("im_bot"),

    /** Scheduled / automation runs. */
    AUTOMATION("automation");

    private final String code;

    AgentChannel(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
