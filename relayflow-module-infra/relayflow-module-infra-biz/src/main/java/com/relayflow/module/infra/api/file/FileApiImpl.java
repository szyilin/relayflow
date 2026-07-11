package com.relayflow.module.infra.api.file;

import com.relayflow.module.infra.api.file.dto.FileBindReqDTO;
import com.relayflow.module.infra.api.file.dto.FileRespDTO;
import com.relayflow.module.infra.service.file.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FileApiImpl implements FileApi {

    private final FileService fileService;

    @Override
    public FileRespDTO getFile(Long fileId) {
        return fileService.getFile(fileId);
    }

    @Override
    public void bindFile(FileBindReqDTO request) {
        fileService.bindFile(request);
    }
}
