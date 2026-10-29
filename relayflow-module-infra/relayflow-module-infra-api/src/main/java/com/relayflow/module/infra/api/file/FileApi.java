package com.relayflow.module.infra.api.file;

import com.relayflow.module.infra.api.file.dto.FileBindReqDTO;
import com.relayflow.module.infra.api.file.dto.FileRespDTO;

import java.util.Collection;
import java.util.Map;

public interface FileApi {

    FileRespDTO getFile(Long fileId);

    /**
     * Batch lookup by file id. Missing ids are omitted from the result map.
     */
    Map<Long, FileRespDTO> getFiles(Collection<Long> fileIds);

    FileRespDTO requireReadable(Long tenantId, Long userId, Long fileId);

    void bindFile(FileBindReqDTO request);
}
