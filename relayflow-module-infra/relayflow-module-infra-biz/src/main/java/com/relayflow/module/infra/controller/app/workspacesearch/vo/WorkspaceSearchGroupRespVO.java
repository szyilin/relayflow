package com.relayflow.module.infra.controller.app.workspacesearch.vo;

import lombok.Data;

import java.util.List;

@Data
public class WorkspaceSearchGroupRespVO {

    private String type;
    private String label;
    private List<WorkspaceSearchItemRespVO> items;
}
