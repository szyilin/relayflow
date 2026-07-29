package com.relayflow.module.calendar.service.settings;

import com.relayflow.module.calendar.controller.admin.vo.CalProductSettingsRespVO;
import com.relayflow.module.calendar.controller.admin.vo.CalProductSettingsUpdateReqVO;

public interface CalTenantSettingsService {

    CalProductSettingsRespVO getSettings();

    CalProductSettingsRespVO updateSettings(CalProductSettingsUpdateReqVO request);

    boolean allowMemberCreateOwnedCalendar(Long tenantId);
}
