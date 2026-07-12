package com.relayflow.module.system.controller.admin.user;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.common.pojo.PageResult;
import com.relayflow.module.system.api.user.dto.UserCreateReqDTO;
import com.relayflow.module.system.api.user.dto.UserInviteReqDTO;
import com.relayflow.module.system.controller.admin.user.vo.UserCreateReqVO;
import com.relayflow.module.system.controller.admin.user.vo.UserInviteReqVO;
import com.relayflow.module.system.controller.admin.user.vo.UserGetRespVO;
import com.relayflow.module.system.controller.admin.user.vo.UserPageReqVO;
import com.relayflow.module.system.controller.admin.user.vo.UserRespVO;
import com.relayflow.module.system.controller.admin.user.vo.UserUpdateDeptReqVO;
import com.relayflow.module.system.controller.admin.user.vo.UserUpdateReqVO;
import com.relayflow.module.system.controller.admin.user.vo.UserUpdateRoleReqVO;
import com.relayflow.module.system.controller.admin.user.vo.UserUpdateStatusReqVO;
import com.relayflow.module.system.service.user.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
@RequestMapping("/admin-api/system/user")
public class UserController {

    private final UserService userService;

    @PreAuthorize("hasAuthority('system:user:list')")
    @GetMapping("/page")
    public CommonResult<PageResult<UserRespVO>> getUserPage(@Valid UserPageReqVO request) {
        return CommonResult.success(userService.getUserPage(request));
    }

    @PreAuthorize("hasAuthority('system:user:query')")
    @GetMapping("/get")
    public CommonResult<UserGetRespVO> getUser(@RequestParam("id") @NotNull Long id) {
        return CommonResult.success(userService.getUser(id));
    }

    @PreAuthorize("hasAuthority('system:user:create')")
    @PostMapping("/create")
    public CommonResult<Long> createUser(@Valid @RequestBody UserCreateReqVO request) {
        UserCreateReqDTO dto = new UserCreateReqDTO();
        dto.setUsername(request.getUsername());
        dto.setPassword(request.getPassword());
        dto.setNickname(request.getNickname());
        dto.setMobile(request.getMobile());
        dto.setEmail(request.getEmail());
        dto.setDeptId(request.getDeptId());
        dto.setRoleIds(request.getRoleIds());
        return CommonResult.success(userService.createUser(dto));
    }

    @PreAuthorize("hasAuthority('system:user:create')")
    @PostMapping("/invite")
    public CommonResult<Long> inviteMember(@Valid @RequestBody UserInviteReqVO request) {
        UserInviteReqDTO dto = new UserInviteReqDTO();
        dto.setMobile(request.getMobile());
        dto.setNickname(request.getNickname());
        dto.setEmail(request.getEmail());
        dto.setDeptId(request.getDeptId());
        dto.setRoleIds(request.getRoleIds());
        return CommonResult.success(userService.inviteMember(dto));
    }

    @PreAuthorize("hasAuthority('system:user:update')")
    @PutMapping("/update")
    public CommonResult<Boolean> updateUser(@Valid @RequestBody UserUpdateReqVO request) {
        userService.updateUser(request);
        return CommonResult.success(true);
    }

    @PreAuthorize("hasAuthority('system:user:update')")
    @PutMapping("/update-status")
    public CommonResult<Boolean> updateUserStatus(@Valid @RequestBody UserUpdateStatusReqVO request) {
        userService.updateUserStatus(request);
        return CommonResult.success(true);
    }

    @PreAuthorize("hasAuthority('system:user:update')")
    @PutMapping("/update-dept")
    public CommonResult<Boolean> updateUserDept(@Valid @RequestBody UserUpdateDeptReqVO request) {
        userService.updateUserDept(request);
        return CommonResult.success(true);
    }

    @PreAuthorize("hasAuthority('system:user:update')")
    @PutMapping("/update-role")
    public CommonResult<Boolean> updateUserRole(@Valid @RequestBody UserUpdateRoleReqVO request) {
        userService.updateUserRole(request);
        return CommonResult.success(true);
    }
}
