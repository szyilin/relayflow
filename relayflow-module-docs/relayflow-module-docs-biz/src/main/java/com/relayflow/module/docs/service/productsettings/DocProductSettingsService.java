package com.relayflow.module.docs.service.productsettings;

import com.relayflow.module.docs.controller.admin.vo.DocProductSettingsRespVO;
import com.relayflow.module.docs.controller.app.vo.DocEffectiveSettingsRespVO;

public interface DocProductSettingsService {

    DocProductSettingsRespVO getSettings();

    DocProductSettingsRespVO updateSettings(DocProductSettingsRespVO request);

    DocEffectiveSettingsRespVO getEffectiveSettings();

    boolean resolveMemberShareEnabled(Long tenantId);

    boolean resolveMarkdownExportEnabled(Long tenantId);

    boolean resolveDriveUploadEnabled(Long tenantId);
}
