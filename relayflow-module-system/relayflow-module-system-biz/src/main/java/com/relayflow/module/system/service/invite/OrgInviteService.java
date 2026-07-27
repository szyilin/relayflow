package com.relayflow.module.system.service.invite;

import com.relayflow.module.system.controller.app.vo.AppOrgInviteCapabilityRespVO;
import com.relayflow.module.system.controller.app.vo.AppOrgInviteConsumeReqVO;
import com.relayflow.module.system.controller.app.vo.AppOrgInviteConsumeRespVO;
import com.relayflow.module.system.controller.app.vo.AppOrgInviteMobileCreateReqVO;
import com.relayflow.module.system.controller.app.vo.AppOrgInviteMobileRespVO;
import com.relayflow.module.system.controller.app.vo.AppOrgInviteSharedRespVO;

public interface OrgInviteService {

    AppOrgInviteCapabilityRespVO getCapability();

    AppOrgInviteSharedRespVO getOrCreateShared();

    AppOrgInviteSharedRespVO resetShared();

    AppOrgInviteMobileRespVO createMobileInvite(AppOrgInviteMobileCreateReqVO request);

    AppOrgInviteConsumeRespVO consume(AppOrgInviteConsumeReqVO request);
}
