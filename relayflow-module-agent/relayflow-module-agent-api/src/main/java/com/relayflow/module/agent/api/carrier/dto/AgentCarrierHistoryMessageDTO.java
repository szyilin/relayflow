package com.relayflow.module.agent.api.carrier.dto;

import lombok.Data;

@Data
public class AgentCarrierHistoryMessageDTO {

    /** {@code user} or {@code assistant} */
    private String role;
    private String content;
}
