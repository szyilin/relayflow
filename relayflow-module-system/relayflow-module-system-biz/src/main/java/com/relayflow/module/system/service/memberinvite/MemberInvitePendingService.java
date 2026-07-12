package com.relayflow.module.system.service.memberinvite;

import com.relayflow.module.system.controller.app.vo.MemberInvitePendingListRespVO;

public interface MemberInvitePendingService {

    MemberInvitePendingListRespVO listPendingByMobile(String mobile);
}
