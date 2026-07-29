package com.relayflow.module.docs.controller.admin;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.docs.controller.admin.vo.DocAdminFilePageReqVO;
import com.relayflow.module.docs.controller.admin.vo.DocAdminFilePageRespVO;
import com.relayflow.module.docs.controller.admin.vo.DocAdminObjectIdsReqVO;
import com.relayflow.module.docs.controller.admin.vo.DocAdminRestoreReqVO;
import com.relayflow.module.docs.controller.admin.vo.DocAdminTransferOwnerReqVO;
import com.relayflow.module.docs.service.admin.DocAdminFilesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/docs/files")
public class DocAdminFilesController {

    private final DocAdminFilesService docAdminFilesService;

    @PreAuthorize("hasAuthority('docs:files:query')")
    @GetMapping
    public CommonResult<DocAdminFilePageRespVO> page(@Valid DocAdminFilePageReqVO request) {
        return CommonResult.success(docAdminFilesService.page(request));
    }

    @PreAuthorize("hasAuthority('docs:files:transfer')")
    @PostMapping("/transfer-owner")
    public CommonResult<Boolean> transferOwner(@Valid @RequestBody DocAdminTransferOwnerReqVO request) {
        docAdminFilesService.transferOwner(request);
        return CommonResult.success(true);
    }

    @PreAuthorize("hasAuthority('docs:files:delete')")
    @PostMapping("/delete")
    public CommonResult<Boolean> delete(@Valid @RequestBody DocAdminObjectIdsReqVO request) {
        docAdminFilesService.delete(request);
        return CommonResult.success(true);
    }

    @PreAuthorize("hasAuthority('docs:files:restore')")
    @PostMapping("/restore")
    public CommonResult<Boolean> restore(@Valid @RequestBody DocAdminRestoreReqVO request) {
        docAdminFilesService.restore(request);
        return CommonResult.success(true);
    }

    @PreAuthorize("hasAuthority('docs:files:purge')")
    @PostMapping("/purge")
    public CommonResult<Boolean> purge(@Valid @RequestBody DocAdminObjectIdsReqVO request) {
        docAdminFilesService.purge(request);
        return CommonResult.success(true);
    }
}
