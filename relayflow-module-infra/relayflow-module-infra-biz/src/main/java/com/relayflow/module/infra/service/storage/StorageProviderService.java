package com.relayflow.module.infra.service.storage;

import com.relayflow.module.infra.controller.admin.storage.vo.StorageConfigRespVO;
import com.relayflow.module.infra.controller.admin.storage.vo.StorageEffectiveSourceReqVO;
import com.relayflow.module.infra.controller.admin.storage.vo.StorageProviderSaveReqVO;
import com.relayflow.module.infra.controller.admin.storage.vo.StorageTestConnectionReqVO;

public interface StorageProviderService {

    StorageConfigRespVO getConfig();

    void saveConfig(StorageProviderSaveReqVO request);

    void setEffectiveSource(StorageEffectiveSourceReqVO request);

    void deleteConfig(String provider);

    void testConnection(StorageTestConnectionReqVO request);
}
