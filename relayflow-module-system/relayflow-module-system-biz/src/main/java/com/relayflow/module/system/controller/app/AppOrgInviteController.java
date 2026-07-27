package com.relayflow.module.system.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.system.controller.app.vo.AppOrgInviteCapabilityRespVO;
import com.relayflow.module.system.controller.app.vo.AppOrgInviteConsumeReqVO;
import com.relayflow.module.system.controller.app.vo.AppOrgInviteConsumeRespVO;
import com.relayflow.module.system.controller.app.vo.AppOrgInviteMobileCreateReqVO;
import com.relayflow.module.system.controller.app.vo.AppOrgInviteMobileRespVO;
import com.relayflow.module.system.controller.app.vo.AppOrgInviteSharedRespVO;
import com.relayflow.module.system.service.invite.OrgInviteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/system/org-invite")
public class AppOrgInviteController {

    private final OrgInviteService orgInviteService;

    @GetMapping("/capability")
    public CommonResult<AppOrgInviteCapabilityRespVO> capability() {
        return CommonResult.success(orgInviteService.getCapability());
    }

    @GetMapping("/shared")
    public CommonResult<AppOrgInviteSharedRespVO> getShared() {
        return CommonResult.success(orgInviteService.getOrCreateShared());
    }

    @PostMapping("/shared/reset")
    public CommonResult<AppOrgInviteSharedRespVO> resetShared() {
        return CommonResult.success(orgInviteService.resetShared());
    }

    @PostMapping("/mobile")
    public CommonResult<AppOrgInviteMobileRespVO> createMobile(
            @Valid @RequestBody AppOrgInviteMobileCreateReqVO request) {
        return CommonResult.success(orgInviteService.createMobileInvite(request));
    }

    @PostMapping("/consume")
    public CommonResult<AppOrgInviteConsumeRespVO> consume(
            @Valid @RequestBody AppOrgInviteConsumeReqVO request) {
        return CommonResult.success(orgInviteService.consume(request));
    }
}
