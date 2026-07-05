package com.relayflow.module.system.controller.admin.user;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.common.pojo.PageResult;
import com.relayflow.module.system.api.user.dto.UserCreateReqDTO;
import com.relayflow.module.system.controller.admin.user.vo.UserPageReqVO;
import com.relayflow.module.system.controller.admin.user.vo.UserRespVO;
import com.relayflow.module.system.service.user.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @PreAuthorize("hasAuthority('system:user:create')")
    @PostMapping("/create")
    public CommonResult<Long> createUser(@Valid @RequestBody UserCreateReqVO request) {
        UserCreateReqDTO dto = new UserCreateReqDTO();
        dto.setUsername(request.getUsername());
        dto.setPassword(request.getPassword());
        dto.setNickname(request.getNickname());
        dto.setMobile(request.getMobile());
        dto.setEmail(request.getEmail());
        return CommonResult.success(userService.createUser(dto));
    }

    @Data
    public static class UserCreateReqVO {

        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;

        private String nickname;
        private String mobile;
        private String email;
    }
}
