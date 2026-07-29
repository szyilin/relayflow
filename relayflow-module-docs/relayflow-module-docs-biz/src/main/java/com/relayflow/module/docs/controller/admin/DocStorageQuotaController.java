package com.relayflow.module.docs.controller.admin;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.docs.controller.admin.vo.DocStorageQuotaRespVO;
import com.relayflow.module.docs.controller.admin.vo.DocStorageUsagePageReqVO;
import com.relayflow.module.docs.controller.admin.vo.DocStorageUsagePageRespVO;
import com.relayflow.module.docs.service.storage.DocStorageQuotaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/docs")
public class DocStorageQuotaController {

    private final DocStorageQuotaService docStorageQuotaService;

    @PreAuthorize("hasAuthority('docs:storage-quota:query')")
    @GetMapping("/storage-usage")
    public CommonResult<DocStorageUsagePageRespVO> pageUsage(@Valid DocStorageUsagePageReqVO request) {
        return CommonResult.success(docStorageQuotaService.pageUsage(request));
    }

    @PreAuthorize("hasAuthority('docs:storage-quota:query')")
    @GetMapping("/storage-quota")
    public CommonResult<DocStorageQuotaRespVO> getQuota() {
        return CommonResult.success(docStorageQuotaService.getQuota());
    }

    @PreAuthorize("hasAuthority('docs:storage-quota:update')")
    @PutMapping("/storage-quota")
    public CommonResult<DocStorageQuotaRespVO> updateQuota(@Valid @RequestBody DocStorageQuotaRespVO request) {
        return CommonResult.success(docStorageQuotaService.updateQuota(request));
    }
}
