package com.relayflow.module.docs.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.docs.controller.app.vo.DocEffectiveSettingsRespVO;
import com.relayflow.module.docs.service.productsettings.DocProductSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/docs/settings")
public class DocSettingsController {

    private final DocProductSettingsService productSettingsService;

    @GetMapping("/effective")
    public CommonResult<DocEffectiveSettingsRespVO> getEffectiveSettings() {
        return CommonResult.success(productSettingsService.getEffectiveSettings());
    }
}
