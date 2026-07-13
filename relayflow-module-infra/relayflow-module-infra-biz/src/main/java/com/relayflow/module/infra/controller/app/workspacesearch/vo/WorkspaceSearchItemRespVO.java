package com.relayflow.module.infra.controller.app.workspacesearch.vo;

import lombok.Data;

import java.util.List;

@Data
public class WorkspaceSearchItemRespVO {

    private String id;
    private String title;
    private String subtitle;
    private String route;
    private String entityType;
    private String entityId;
}
