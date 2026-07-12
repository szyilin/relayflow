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

/**
 * V1 邀请接受 API。V2（{@code tenant.enabled=true}）请使用 {@code POST /app-api/system/auth/register}；
 * 本组 endpoint 保留兼容 {@code enabled=false} 自托管场景。
 *
 * @deprecated 使用开放注册 {@code /app-api/system/auth/register} 替代 preview/accept
 */
@Deprecated(since = "0.2.0")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/app-api/system/member-invite")
public class AppMemberInviteController {

    private final MemberInviteService memberInviteService;

    /**
     * @deprecated V2 请跳转 {@code /app/register} 并完成开放注册
     */
    @Deprecated(since = "0.2.0")
    @GetMapping("/preview")
    public CommonResult<MemberInvitePreviewRespVO> preview(
            @RequestParam @NotBlank(message = "手机号不能为空") String mobile) {
        return CommonResult.success(memberInviteService.preview(mobile));
    }

    /**
     * @deprecated V2 邀请激活已合并至 {@code AuthRegisterService}
     */
    @Deprecated(since = "0.2.0")
    @PostMapping("/accept")
    public CommonResult<AuthLoginRespVO> accept(@Valid @RequestBody MemberInviteAcceptReqVO request) {
        return CommonResult.success(memberInviteService.accept(request.getMobile(), request.getPassword()));
    }
}
