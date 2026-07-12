package com.relayflow.module.infra.service.file;

import com.relayflow.module.infra.api.file.dto.FileBindReqDTO;
import com.relayflow.module.infra.api.file.dto.FileRespDTO;
import com.relayflow.module.infra.controller.admin.file.vo.FileListItemRespVO;
import com.relayflow.module.infra.controller.admin.file.vo.FilePageReqVO;
import com.relayflow.common.pojo.PageResult;
import com.relayflow.module.infra.dal.dataobject.InfraFileDO;
import com.relayflow.module.infra.dal.dataobject.InfraFileUploadSessionDO;
import com.relayflow.framework.oss.core.model.StorageObjectMeta;
import com.relayflow.framework.oss.core.model.StorageProviderConfig;

public interface FileService {

    FileRespDTO getFile(Long fileId);

    PageResult<FileListItemRespVO> getFilePage(FilePageReqVO request);

    void deleteFile(Long fileId);

    InfraFileDO requireFile(Long fileId);

    /**
     * Resolve a public file by global id (ignores tenant context). Used by permitAll public download.
     */
    InfraFileDO requirePublicFile(Long fileId);

    InfraFileDO createFromSession(InfraFileUploadSessionDO session,
                                  StorageProviderConfig providerConfig,
                                  StorageObjectMeta objectMeta);

    void bindFile(FileBindReqDTO request);
}
