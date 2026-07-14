package com.relayflow.module.infra.controller.app.workspacesearch;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.infra.controller.app.workspacesearch.vo.WorkspaceSearchRespVO;
import com.relayflow.module.infra.service.workspacesearch.WorkspaceSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/infra/workspace-search")
public class AppWorkspaceSearchController {

    private final WorkspaceSearchService workspaceSearchService;

    @GetMapping
    public CommonResult<WorkspaceSearchRespVO> search(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "limitPerGroup", defaultValue = "5") int limitPerGroup) {
        return CommonResult.success(workspaceSearchService.search(keyword, limitPerGroup));
    }
}
