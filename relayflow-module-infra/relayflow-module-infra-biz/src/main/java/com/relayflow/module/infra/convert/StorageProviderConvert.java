package com.relayflow.module.infra.convert;

import com.relayflow.module.infra.controller.admin.storage.vo.StorageProviderRespVO;
import com.relayflow.module.infra.dal.dataobject.InfraStorageProviderDO;
import com.relayflow.module.infra.service.storage.model.StorageProviderConfigJson;

public final class StorageProviderConvert {

    private StorageProviderConvert() {
    }

    public static StorageProviderRespVO toVo(InfraStorageProviderDO row, StorageProviderConfigJson configJson) {
        StorageProviderRespVO vo = new StorageProviderRespVO();
        vo.setProvider(row.getProvider());
        vo.setStatus(row.getStatus());
        vo.setIsDefault(Integer.valueOf(1).equals(row.getIsDefault()));
        if (configJson != null) {
            vo.setEndpoint(configJson.getEndpoint());
            vo.setBucket(configJson.getBucket());
            vo.setAccessKey(configJson.getAccessKey());
            vo.setUseSsl(Boolean.TRUE.equals(configJson.getUseSsl()));
            vo.setPathPrefix(configJson.getPathPrefix());
            vo.setSecretKeyConfigured(configJson.getSecretKeyEnc() != null && !configJson.getSecretKeyEnc().isBlank());
        } else {
            vo.setSecretKeyConfigured(false);
        }
        return vo;
    }
}
