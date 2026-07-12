package com.relayflow.module.system.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.common.util.MobileUtils;
import com.relayflow.module.system.controller.app.vo.MemberInvitePendingListRespVO;
import com.relayflow.module.system.service.memberinvite.MemberInvitePendingService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/app-api/system/member-invite")
public class AppMemberInvitePendingController {

    private final MemberInvitePendingService memberInvitePendingService;

    @GetMapping("/pending")
    public CommonResult<MemberInvitePendingListRespVO> pending(
            @RequestParam @NotBlank(message = "手机号不能为空") String mobile) {
        return CommonResult.success(memberInvitePendingService.listPendingByMobile(MobileUtils.normalize(mobile)));
    }
}
