package com.relayflow.module.agent.controller.app.skill.vo;

import lombok.Data;

@Data
public class SkillCatalogItemRespVO {

    private String code;
    private String name;
    private String description;
    private String inputSchema;
    private String domain;
    /** HOT | DEFERRED */
    private String tier;
}
