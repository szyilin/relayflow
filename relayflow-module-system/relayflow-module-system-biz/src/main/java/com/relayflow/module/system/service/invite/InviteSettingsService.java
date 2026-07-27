package com.relayflow.module.system.service.invite;

import com.relayflow.module.system.controller.admin.invite.vo.InviteSettingsRespVO;
import com.relayflow.module.system.controller.admin.invite.vo.InviteSettingsUpdateReqVO;

public interface InviteSettingsService {

    InviteSettingsRespVO getSettings();

    InviteSettingsRespVO getSettings(long tenantId);

    InviteSettingsRespVO updateSettings(InviteSettingsUpdateReqVO request);
}
