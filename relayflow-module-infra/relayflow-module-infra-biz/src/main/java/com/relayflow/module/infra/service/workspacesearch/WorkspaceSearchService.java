package com.relayflow.module.infra.service.workspacesearch;

import com.relayflow.module.infra.controller.app.workspacesearch.vo.WorkspaceSearchRespVO;

public interface WorkspaceSearchService {

    WorkspaceSearchRespVO search(String keyword, int limitPerGroup);
}
