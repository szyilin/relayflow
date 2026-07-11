package com.relayflow.module.infra.api.file;

import com.relayflow.module.infra.api.file.dto.FileBindReqDTO;
import com.relayflow.module.infra.api.file.dto.FileRespDTO;

public interface FileApi {

    FileRespDTO getFile(Long fileId);

    void bindFile(FileBindReqDTO request);
}
