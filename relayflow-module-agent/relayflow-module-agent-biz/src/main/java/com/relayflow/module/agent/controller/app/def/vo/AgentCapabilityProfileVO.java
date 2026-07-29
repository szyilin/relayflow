package com.relayflow.module.agent.controller.app.def.vo;

import lombok.Data;

import java.util.List;

@Data
public class AgentCapabilityProfileVO {

    private List<String> hotPacks;
    private List<String> deferredDomains;
}
