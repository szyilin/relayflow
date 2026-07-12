package com.relayflow.module.system.service.memberinvite;

import com.relayflow.module.system.controller.admin.auth.vo.AuthLoginRespVO;
import com.relayflow.module.system.controller.app.vo.MemberInvitePreviewRespVO;

public interface MemberInviteService {

    MemberInvitePreviewRespVO preview(String mobile);

    AuthLoginRespVO accept(String mobile, String password);
}
