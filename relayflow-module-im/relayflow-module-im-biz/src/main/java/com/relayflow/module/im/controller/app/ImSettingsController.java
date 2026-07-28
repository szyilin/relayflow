package com.relayflow.module.im.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.im.controller.app.vo.ImEffectiveSettingsRespVO;
import com.relayflow.module.im.service.productsettings.ImProductSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/im/settings")
public class ImSettingsController {

    private final ImProductSettingsService productSettingsService;

    @GetMapping("/effective")
    public CommonResult<ImEffectiveSettingsRespVO> getEffectiveSettings() {
        return CommonResult.success(productSettingsService.getEffectiveSettings());
    }
}
