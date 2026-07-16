package com.relayflow.module.system.service.preference;

import com.relayflow.module.system.controller.app.vo.AppUserPreferenceRespVO;
import com.relayflow.module.system.controller.app.vo.AppUserPreferenceUpdateReqVO;

public interface UserPreferenceService {

    AppUserPreferenceRespVO getMyPreference();

    AppUserPreferenceRespVO updateMyPreference(AppUserPreferenceUpdateReqVO request);
}
