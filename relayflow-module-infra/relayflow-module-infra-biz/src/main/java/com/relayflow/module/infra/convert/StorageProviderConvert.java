package com.relayflow.module.infra.convert;

import com.relayflow.module.infra.controller.admin.storage.vo.StorageProviderRespVO;
import com.relayflow.module.infra.dal.dataobject.InfraStorageProviderDO;
import com.relayflow.module.infra.service.storage.model.StorageProviderConfigJson;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface StorageProviderConvert {

    StorageProviderConvert INSTANCE = Mappers.getMapper(StorageProviderConvert.class);

    @Mapping(target = "isDefault", expression = "java(Integer.valueOf(1).equals(row.getIsDefault()))")
    @Mapping(target = "endpoint", source = "configJson.endpoint")
    @Mapping(target = "bucket", source = "configJson.bucket")
    @Mapping(target = "accessKey", source = "configJson.accessKey")
    @Mapping(target = "useSsl", source = "configJson.useSsl")
    @Mapping(target = "pathPrefix", source = "configJson.pathPrefix")
    @Mapping(target = "secretKeyConfigured", expression = "java(isSecretKeyConfigured(configJson))")
    StorageProviderRespVO toVo(InfraStorageProviderDO row, StorageProviderConfigJson configJson);

    default boolean isSecretKeyConfigured(StorageProviderConfigJson configJson) {
        return configJson != null
                && configJson.getSecretKeyEnc() != null
                && !configJson.getSecretKeyEnc().isBlank();
    }
}
