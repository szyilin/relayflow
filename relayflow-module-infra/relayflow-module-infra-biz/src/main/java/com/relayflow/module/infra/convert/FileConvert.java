package com.relayflow.module.infra.convert;

import com.relayflow.module.infra.api.file.dto.FileRespDTO;
import com.relayflow.module.infra.dal.dataobject.InfraFileDO;

public final class FileConvert {

    private FileConvert() {
    }

    public static FileRespDTO toDto(InfraFileDO file) {
        if (file == null) {
            return null;
        }
        FileRespDTO dto = new FileRespDTO();
        dto.setId(file.getId());
        dto.setTenantId(file.getTenantId());
        dto.setProvider(file.getProvider());
        dto.setStorageUri(file.getStorageUri());
        dto.setObjectKey(file.getObjectKey());
        dto.setOriginalName(file.getOriginalName());
        dto.setMimeType(file.getMimeType());
        dto.setSize(file.getSize());
        dto.setSha256(file.getSha256());
        dto.setAccessLevel(file.getAccessLevel());
        dto.setCreateTime(file.getCreateTime());
        return dto;
    }
}
