package com.relayflow.module.system.controller.admin.permission;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.system.controller.admin.permission.vo.PermissionRespVO;
import com.relayflow.module.system.service.permission.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/system/permission")
public class PermissionController {

    private final PermissionService permissionService;

    @PreAuthorize("hasAuthority('system:role:query')")
    @GetMapping("/list")
    public CommonResult<List<PermissionRespVO>> getPermissionList() {
        return CommonResult.success(permissionService.getPermissionTree());
    }
}
