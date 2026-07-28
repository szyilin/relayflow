package com.relayflow.module.im.controller.admin.productsettings;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.im.controller.admin.productsettings.vo.ImGroupSearchItemRespVO;
import com.relayflow.module.im.controller.admin.productsettings.vo.ImProductSettingsRespVO;
import com.relayflow.module.im.service.productsettings.ImProductSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/im/product-settings")
public class ImProductSettingsController {

    private final ImProductSettingsService productSettingsService;

    @PreAuthorize("hasAuthority('im:product-settings:query')")
    @GetMapping
    public CommonResult<ImProductSettingsRespVO> getSettings() {
        return CommonResult.success(productSettingsService.getSettings());
    }

    @PreAuthorize("hasAuthority('im:product-settings:update')")
    @PutMapping
    public CommonResult<ImProductSettingsRespVO> updateSettings(
            @Valid @RequestBody ImProductSettingsRespVO request) {
        return CommonResult.success(productSettingsService.updateSettings(request));
    }

    @PreAuthorize("hasAuthority('im:product-settings:query')")
    @GetMapping("/groups")
    public CommonResult<List<ImGroupSearchItemRespVO>> searchGroups(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return CommonResult.success(productSettingsService.searchGroups(keyword));
    }
}
