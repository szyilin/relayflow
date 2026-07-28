package com.relayflow.module.im.service.productsettings;

import com.relayflow.module.im.controller.admin.productsettings.vo.ImGroupSearchItemRespVO;
import com.relayflow.module.im.controller.admin.productsettings.vo.ImProductSettingsRespVO;
import com.relayflow.module.im.controller.app.vo.ImEffectiveSettingsRespVO;

import java.util.List;

public interface ImProductSettingsService {

    ImProductSettingsRespVO getSettings();

    ImProductSettingsRespVO getSettings(Long tenantId);

    ImProductSettingsRespVO updateSettings(ImProductSettingsRespVO request);

    List<ImGroupSearchItemRespVO> searchGroups(String keyword);

    ImEffectiveSettingsRespVO getEffectiveSettings();

    int resolveMemberLimit(Long tenantId, Long conversationId, Long userId);
}
