package com.relayflow.module.infra.service.file;

import com.relayflow.module.infra.api.file.dto.FileBindReqDTO;
import com.relayflow.module.infra.api.file.dto.FileRespDTO;
import com.relayflow.module.infra.dal.dataobject.InfraFileDO;
import com.relayflow.module.infra.dal.dataobject.InfraFileUploadSessionDO;
import com.relayflow.framework.oss.core.model.StorageObjectMeta;
import com.relayflow.framework.oss.core.model.StorageProviderConfig;

public interface FileService {

    FileRespDTO getFile(Long fileId);

    InfraFileDO requireFile(Long fileId);

    InfraFileDO createFromSession(InfraFileUploadSessionDO session,
                                  StorageProviderConfig providerConfig,
                                  StorageObjectMeta objectMeta);

    void bindFile(FileBindReqDTO request);
}
