package com.relayflow.module.agent.controller.app.session.vo;

import lombok.Data;

import java.util.List;

@Data
public class AgentSessionMessageRespVO {

    private Long id;
    private String role;
    private String content;
    private Integer seq;
    /** Optional parts; omitted/null when parts_json empty. */
    private List<AgentMessagePartVO> parts;
}
