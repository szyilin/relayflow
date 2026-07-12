package com.relayflow.module.system.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.system.controller.app.vo.AuthRegisterReqVO;
import com.relayflow.module.system.controller.app.vo.AuthRegisterRespVO;
import com.relayflow.module.system.service.auth.AuthRegisterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/system/auth")
public class AppAuthController {

    private final AuthRegisterService authRegisterService;

    @PostMapping("/register")
    public CommonResult<AuthRegisterRespVO> register(@Valid @RequestBody AuthRegisterReqVO request) {
        return CommonResult.success(authRegisterService.register(request));
    }
}
