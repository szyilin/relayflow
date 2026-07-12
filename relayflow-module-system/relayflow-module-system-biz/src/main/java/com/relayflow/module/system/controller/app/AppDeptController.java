package com.relayflow.module.system.controller.app;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.CommonResult;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.system.controller.admin.dept.vo.DeptRespVO;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import com.relayflow.module.system.service.dept.DeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/system/dept")
public class AppDeptController {

    private final DeptService deptService;

    @GetMapping("/tree")
    public CommonResult<List<DeptRespVO>> getDeptTree() {
        requireLoginUser();
        return CommonResult.success(deptService.getEnabledDeptList());
    }

    private LoginUser requireLoginUser() {
        LoginUser loginUser = SecurityFrameworkUtils.getLoginUser();
        if (loginUser == null) {
            throw new ServiceException(ErrorCodeConstants.AUTH_LOGIN_BAD_CREDENTIALS);
        }
        return loginUser;
    }
}
