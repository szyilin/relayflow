package com.relayflow.module.docs.controller.admin;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.docs.controller.admin.vo.DocProductSettingsRespVO;
import com.relayflow.module.docs.service.productsettings.DocProductSettingsService;
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
@RequestMapping("/admin-api/docs/product-settings")
public class DocProductSettingsController {

    private final DocProductSettingsService productSettingsService;

    @PreAuthorize("hasAuthority('docs:product-settings:query')")
    @GetMapping
    public CommonResult<DocProductSettingsRespVO> getSettings() {
        return CommonResult.success(productSettingsService.getSettings());
    }

    @PreAuthorize("hasAuthority('docs:product-settings:update')")
    @PutMapping
    public CommonResult<DocProductSettingsRespVO> updateSettings(
            @Valid @RequestBody DocProductSettingsRespVO request) {
        return CommonResult.success(productSettingsService.updateSettings(request));
    }
}
