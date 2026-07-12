package com.relayflow.module.system.controller.app;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.CommonResult;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.system.controller.app.vo.AppContactItemRespVO;
import com.relayflow.module.system.controller.app.vo.AppUserProfileRespVO;
import com.relayflow.module.system.controller.app.vo.AppUserProfileUpdateReqVO;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import com.relayflow.module.system.service.user.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/app-api/system/user")
public class AppUserController {

    private final UserService userService;

    @GetMapping("/list-by-dept")
    public CommonResult<List<AppContactItemRespVO>> listContactsByDept(
            @RequestParam @NotNull Long deptId,
            @RequestParam(value = "keyword", required = false) String keyword) {
        LoginUser loginUser = requireLoginUser();
        return CommonResult.success(userService.listContactsByDept(deptId, keyword));
    }

    @GetMapping("/profile")
    public CommonResult<AppUserProfileRespVO> getMyProfile() {
        LoginUser loginUser = requireLoginUser();
        return CommonResult.success(userService.getMyProfile(loginUser.getUserId(), loginUser.getTenantId()));
    }

    @PutMapping("/profile")
    public CommonResult<AppUserProfileRespVO> updateMyProfile(
            @Valid @RequestBody AppUserProfileUpdateReqVO request) {
        LoginUser loginUser = requireLoginUser();
        return CommonResult.success(
                userService.updateMyProfile(loginUser.getUserId(), loginUser.getTenantId(), request));
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            throw new ServiceException(ErrorCodeConstants.AUTH_LOGIN_BAD_CREDENTIALS);
        }
        return loginUser;
    }
}
