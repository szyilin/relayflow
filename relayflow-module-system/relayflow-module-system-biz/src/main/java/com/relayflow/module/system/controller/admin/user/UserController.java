package com.relayflow.module.system.controller.admin.user;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.system.api.user.dto.UserCreateReqDTO;
import com.relayflow.module.system.service.user.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/system/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

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

    public static class UserCreateReqVO {

        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;

        private String nickname;
        private String mobile;
        private String email;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
