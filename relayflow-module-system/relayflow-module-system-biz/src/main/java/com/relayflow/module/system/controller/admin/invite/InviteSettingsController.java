package com.relayflow.module.system.controller.admin.invite;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.system.controller.admin.invite.vo.InviteSettingsRespVO;
import com.relayflow.module.system.controller.admin.invite.vo.InviteSettingsUpdateReqVO;
import com.relayflow.module.system.service.invite.InviteSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin-api/system/invite-settings")
public class InviteSettingsController {

    private final InviteSettingsService inviteSettingsService;

    @PreAuthorize("hasAuthority('system:user:list')")
    @GetMapping
    public CommonResult<InviteSettingsRespVO> getSettings() {
        return CommonResult.success(inviteSettingsService.getSettings());
    }

    @PreAuthorize("hasAuthority('system:user:create')")
    @PutMapping
    public CommonResult<InviteSettingsRespVO> updateSettings(
            @Valid @RequestBody InviteSettingsUpdateReqVO request) {
        return CommonResult.success(inviteSettingsService.updateSettings(request));
    }
}
