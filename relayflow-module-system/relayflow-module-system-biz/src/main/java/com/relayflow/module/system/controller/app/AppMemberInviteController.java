package com.relayflow.module.system.controller.app;

import com.relayflow.common.pojo.CommonResult;
import com.relayflow.module.system.controller.admin.auth.vo.AuthLoginRespVO;
import com.relayflow.module.system.controller.app.vo.MemberInviteAcceptReqVO;
import com.relayflow.module.system.controller.app.vo.MemberInvitePreviewRespVO;
import com.relayflow.module.system.service.memberinvite.MemberInviteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/system/member-invite")
public class AppMemberInviteController {

    private final MemberInviteService memberInviteService;

    @GetMapping("/preview")
    public CommonResult<MemberInvitePreviewRespVO> preview(
            @RequestParam @NotBlank(message = "手机号不能为空") String mobile) {
        return CommonResult.success(memberInviteService.preview(mobile));
    }

    @PostMapping("/accept")
    public CommonResult<AuthLoginRespVO> accept(@Valid @RequestBody MemberInviteAcceptReqVO request) {
        return CommonResult.success(memberInviteService.accept(request.getMobile(), request.getPassword()));
    }
}
