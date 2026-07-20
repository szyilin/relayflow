package com.relayflow.module.docs.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.docs.controller.app.vo.DocLibraryNodeCreateReqVO;
import com.relayflow.module.docs.controller.app.vo.DocLibraryNodeSummaryRespVO;
import com.relayflow.module.docs.controller.app.vo.DocLibraryNodeUpdateReqVO;
import com.relayflow.module.docs.controller.app.vo.DocLibraryTreeRespVO;
import com.relayflow.module.docs.service.library.DocLibraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/docs/library")
public class DocLibraryController {

    private final DocLibraryService docLibraryService;

    @GetMapping("/tree")
    public CommonResult<DocLibraryTreeRespVO> tree() {
        return CommonResult.success(docLibraryService.getTree());
    }

    @PostMapping("/nodes")
    public CommonResult<DocLibraryNodeSummaryRespVO> createNode(
            @Valid @RequestBody DocLibraryNodeCreateReqVO request) {
        return CommonResult.success(docLibraryService.createNode(request));
    }

    @PutMapping("/nodes/{nodeId}")
    public CommonResult<DocLibraryNodeSummaryRespVO> updateNode(
            @PathVariable Long nodeId,
            @RequestBody DocLibraryNodeUpdateReqVO request) {
        return CommonResult.success(docLibraryService.updateNode(nodeId, request));
    }

    @DeleteMapping("/nodes/{nodeId}")
    public CommonResult<Boolean> deleteNode(@PathVariable Long nodeId) {
        docLibraryService.deleteNode(nodeId);
        return CommonResult.success(true);
    }
}
