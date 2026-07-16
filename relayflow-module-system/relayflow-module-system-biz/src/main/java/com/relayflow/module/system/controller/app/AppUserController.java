package com.relayflow.module.system.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.system.controller.app.vo.AppContactItemRespVO;
import com.relayflow.module.system.controller.app.vo.AppUserPreferenceRespVO;
import com.relayflow.module.system.controller.app.vo.AppUserPreferenceUpdateReqVO;
import com.relayflow.module.system.controller.app.vo.AppUserProfileRespVO;
import com.relayflow.module.system.controller.app.vo.AppUserProfileUpdateReqVO;
import com.relayflow.module.system.service.preference.UserPreferenceService;
import com.relayflow.module.system.service.user.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final UserPreferenceService userPreferenceService;

    @GetMapping("/list-by-dept")
    public CommonResult<List<AppContactItemRespVO>> listContactsByDept(
            @RequestParam @NotNull Long deptId,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return CommonResult.success(userService.listContactsByDept(deptId, keyword));
    }

    @GetMapping("/profile")
    public CommonResult<AppUserProfileRespVO> getMyProfile() {
        return CommonResult.success(userService.getMyProfile());
    }

    @GetMapping("/profile/{userId}")
    public CommonResult<AppUserProfileRespVO> getMemberProfile(
            @PathVariable @NotNull Long userId) {
        return CommonResult.success(userService.getMemberProfile(userId));
    }

    @PutMapping("/profile")
    public CommonResult<AppUserProfileRespVO> updateMyProfile(
            @Valid @RequestBody AppUserProfileUpdateReqVO request) {
        return CommonResult.success(userService.updateMyProfile(request));
    }

    @GetMapping("/preference")
    public CommonResult<AppUserPreferenceRespVO> getMyPreference() {
        return CommonResult.success(userPreferenceService.getMyPreference());
    }

    @PutMapping("/preference")
    public CommonResult<AppUserPreferenceRespVO> updateMyPreference(
            @Valid @RequestBody AppUserPreferenceUpdateReqVO request) {
        return CommonResult.success(userPreferenceService.updateMyPreference(request));
    }
}
