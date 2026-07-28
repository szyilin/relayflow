package com.relayflow.module.agent.controller.app.session.vo;

import lombok.Data;

@Data
public class AgentSessionMessageRespVO {

    private Long id;
    private String role;
    private String content;
    private Integer seq;
}
