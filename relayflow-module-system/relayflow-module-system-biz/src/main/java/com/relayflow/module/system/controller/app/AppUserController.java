package com.relayflow.module.system.controller.app;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.CommonResult;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.system.controller.app.vo.AppContactItemRespVO;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import com.relayflow.module.system.service.user.UserService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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
        requireLoginUser();
        return CommonResult.success(userService.listContactsByDept(deptId, keyword));
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            throw new ServiceException(ErrorCodeConstants.AUTH_LOGIN_BAD_CREDENTIALS);
        }
        return loginUser;
    }
}
