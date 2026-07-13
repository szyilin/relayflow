package com.relayflow.module.infra.controller.app.workspacesearch.vo;

import lombok.Data;

import java.util.List;

@Data
public class WorkspaceSearchRespVO {

    private String keyword;
    private List<WorkspaceSearchGroupRespVO> groups;
}
