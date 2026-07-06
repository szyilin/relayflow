package com.relayflow.module.system.controller.admin.role;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.common.pojo.PageResult;
import com.relayflow.module.system.controller.admin.role.vo.RoleCreateReqVO;
import com.relayflow.module.system.controller.admin.role.vo.RolePageReqVO;
import com.relayflow.module.system.controller.admin.role.vo.RoleRespVO;
import com.relayflow.module.system.controller.admin.role.vo.RoleUpdateReqVO;
import com.relayflow.module.system.service.role.RoleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/system/role")
public class RoleController {

    private final RoleService roleService;

    @PreAuthorize("hasAuthority('system:role:list')")
    @GetMapping("/page")
    public CommonResult<PageResult<RoleRespVO>> getRolePage(@Valid RolePageReqVO request) {
        return CommonResult.success(roleService.getRolePage(request));
    }

    @PreAuthorize("hasAuthority('system:role:query')")
    @GetMapping("/get")
    public CommonResult<RoleRespVO> getRole(@RequestParam @NotNull Long id) {
        return CommonResult.success(roleService.getRole(id));
    }

    @PreAuthorize("hasAuthority('system:role:create')")
    @PostMapping("/create")
    public CommonResult<Long> createRole(@Valid @RequestBody RoleCreateReqVO request) {
        return CommonResult.success(roleService.createRole(request));
    }

    @PreAuthorize("hasAuthority('system:role:update')")
    @PutMapping("/update")
    public CommonResult<Boolean> updateRole(@Valid @RequestBody RoleUpdateReqVO request) {
        roleService.updateRole(request);
        return CommonResult.success(true);
    }

    @PreAuthorize("hasAuthority('system:role:delete')")
    @DeleteMapping("/delete")
    public CommonResult<Boolean> deleteRole(@RequestParam @NotNull Long id) {
        roleService.deleteRole(id);
        return CommonResult.success(true);
    }
}
