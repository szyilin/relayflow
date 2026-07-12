package com.relayflow.module.system.service.memberinvite;

import com.relayflow.module.system.controller.admin.auth.vo.AuthLoginRespVO;
import com.relayflow.module.system.controller.app.vo.MemberInvitePreviewRespVO;

/**
 * V1 组织邀请预览/接受。V2 多租户模式下由 {@code AuthRegisterService} 承接 NOT_JOINED 激活。
 *
 * @deprecated 使用 {@code POST /app-api/system/auth/register} 替代 accept 流程
 */
@Deprecated(since = "0.2.0")
public interface MemberInviteService {

    @Deprecated(since = "0.2.0")
    MemberInvitePreviewRespVO preview(String mobile);

    @Deprecated(since = "0.2.0")
    AuthLoginRespVO accept(String mobile, String password);
}
