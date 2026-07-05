package com.relayflow.module.system.controller.admin.auth;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.CommonResult;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.system.controller.admin.auth.vo.AuthLoginReqVO;
import com.relayflow.module.system.controller.admin.auth.vo.AuthLoginRespVO;
import com.relayflow.module.system.controller.admin.auth.vo.AuthPermissionInfoRespVO;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import com.relayflow.module.system.service.auth.AuthService;
import com.relayflow.module.system.service.permission.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/system/auth")
public class AuthController {

    private final AuthService authService;
    private final PermissionService permissionService;

    @PostMapping("/login")
    public CommonResult<AuthLoginRespVO> login(@Valid @RequestBody AuthLoginReqVO request) {
        return CommonResult.success(authService.login(request));
    }

    @GetMapping("/get-permission-info")
    public CommonResult<AuthPermissionInfoRespVO> getPermissionInfo() {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            throw new ServiceException(ErrorCodeConstants.AUTH_LOGIN_BAD_CREDENTIALS);
        }
        return CommonResult.success(
                permissionService.getPermissionInfo(loginUser.getUserId(), loginUser.getTenantId()));
    }
}
