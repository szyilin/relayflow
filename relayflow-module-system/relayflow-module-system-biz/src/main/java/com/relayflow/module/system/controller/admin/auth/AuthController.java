package com.relayflow.module.system.controller.admin.auth;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.system.controller.admin.auth.vo.AuthLoginReqVO;
import com.relayflow.module.system.controller.admin.auth.vo.AuthLoginRespVO;
import com.relayflow.module.system.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/system/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public CommonResult<AuthLoginRespVO> login(@Valid @RequestBody AuthLoginReqVO request) {
        return CommonResult.success(authService.login(request));
    }
}
