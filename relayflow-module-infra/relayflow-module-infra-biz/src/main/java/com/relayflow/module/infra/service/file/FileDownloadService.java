package com.relayflow.module.infra.service.file;

import com.relayflow.module.infra.service.file.model.FileDownloadRedirect;

public interface FileDownloadService {

    FileDownloadRedirect resolvePublicDownload(Long fileId);

    FileDownloadRedirect resolveAdminDownload(Long fileId);
}
