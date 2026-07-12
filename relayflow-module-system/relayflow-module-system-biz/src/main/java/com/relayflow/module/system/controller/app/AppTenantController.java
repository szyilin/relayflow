package com.relayflow.module.system.controller.app;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.CommonResult;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.system.controller.admin.auth.vo.AuthLoginRespVO;
import com.relayflow.module.system.controller.app.vo.AuthRegisterTenantSummaryVO;
import com.relayflow.module.system.controller.app.vo.TenantSwitchReqVO;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import com.relayflow.module.system.service.tenant.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/system/tenant")
public class AppTenantController {

    private final TenantService tenantService;

    @GetMapping("/my-list")
    public CommonResult<List<AuthRegisterTenantSummaryVO>> myList() {
        LoginUser loginUser = requireLoginUser();
        return CommonResult.success(tenantService.listMyTenants(loginUser.getUserId()));
    }

    @PostMapping("/switch")
    public CommonResult<AuthLoginRespVO> switchTenant(@Valid @RequestBody TenantSwitchReqVO request) {
        LoginUser loginUser = requireLoginUser();
        return CommonResult.success(tenantService.switchTenant(loginUser.getUserId(), request.getTenantId()));
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            throw new ServiceException(ErrorCodeConstants.AUTH_LOGIN_BAD_CREDENTIALS);
        }
        return loginUser;
    }
}
