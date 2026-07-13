package com.relayflow.module.system.service.memberinvite;

import com.relayflow.module.system.controller.app.vo.MemberInvitePendingListRespVO;

@Deprecated(since = "0.2.0", forRemoval = true)
public interface MemberInvitePendingService {

    MemberInvitePendingListRespVO listPendingByMobile(String mobile);
}
