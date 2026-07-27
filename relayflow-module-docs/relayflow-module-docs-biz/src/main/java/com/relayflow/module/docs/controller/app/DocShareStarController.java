package com.relayflow.module.docs.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.docs.controller.app.vo.DocShareCreateReqVO;
import com.relayflow.module.docs.controller.app.vo.DocShareItemRespVO;
import com.relayflow.module.docs.controller.app.vo.DocShareListRespVO;
import com.relayflow.module.docs.controller.app.vo.DocShareUpdateReqVO;
import com.relayflow.module.docs.controller.app.vo.DocSharedListRespVO;
import com.relayflow.module.docs.controller.app.vo.DocStarredListRespVO;
import com.relayflow.module.docs.service.share.DocShareService;
import com.relayflow.module.docs.service.star.DocStarService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/docs")
public class DocShareStarController {

    private final DocShareService docShareService;
    private final DocStarService docStarService;

    @GetMapping("/documents/{objectId}/shares")
    public CommonResult<DocShareListRespVO> listShares(@PathVariable Long objectId) {
        return CommonResult.success(docShareService.listShares(objectId));
    }

    @PostMapping("/documents/{objectId}/shares")
    public CommonResult<DocShareItemRespVO> createShare(
            @PathVariable Long objectId,
            @Valid @RequestBody DocShareCreateReqVO request) {
        return CommonResult.success(docShareService.createShare(objectId, request));
    }

    @PutMapping("/documents/{objectId}/shares/{shareId}")
    public CommonResult<DocShareItemRespVO> updateShare(
            @PathVariable Long objectId,
            @PathVariable Long shareId,
            @Valid @RequestBody DocShareUpdateReqVO request) {
        return CommonResult.success(docShareService.updateShare(objectId, shareId, request));
    }

    @DeleteMapping("/documents/{objectId}/shares/{shareId}")
    public CommonResult<Boolean> revokeShare(
            @PathVariable Long objectId,
            @PathVariable Long shareId) {
        docShareService.revokeShare(objectId, shareId);
        return CommonResult.success(true);
    }

    @GetMapping("/shared")
    public CommonResult<DocSharedListRespVO> listShared(
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return CommonResult.success(docShareService.listSharedWithMe(limit));
    }

    @DeleteMapping("/shared/{objectId}")
    public CommonResult<Boolean> leaveShared(@PathVariable Long objectId) {
        docShareService.leaveShared(objectId);
        return CommonResult.success(true);
    }

    @GetMapping("/starred")
    public CommonResult<DocStarredListRespVO> listStarred(
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        return CommonResult.success(docStarService.listStarred(limit));
    }
}
