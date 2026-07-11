package com.relayflow.module.infra.service.file;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.oss.core.ObjectStorageClient;
import com.relayflow.framework.oss.core.ObjectStorageClientFactory;
import com.relayflow.framework.oss.core.ObjectStorageProviderType;
import com.relayflow.framework.oss.core.model.StorageProviderConfig;
import com.relayflow.module.infra.dal.dataobject.InfraFileDO;
import com.relayflow.module.infra.enums.ErrorCodeConstants;
import com.relayflow.module.infra.service.file.model.FileDownloadRedirect;
import com.relayflow.module.infra.service.storage.StorageProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class FileDownloadServiceImpl implements FileDownloadService {

    private static final String ACCESS_PUBLIC = "public";
    private static final String PUBLIC_CACHE_CONTROL = "public, max-age=31536000, immutable";
    private static final Duration PRIVATE_PRESIGNED_TTL = Duration.ofMinutes(15);
    private static final Duration PUBLIC_PRESIGNED_TTL = Duration.ofMinutes(60);

    private final FileService fileService;
    private final StorageProviderService storageProviderService;
    private final ObjectStorageClientFactory clientFactory;

    @Override
    public FileDownloadRedirect resolvePublicDownload(Long fileId) {
        InfraFileDO file = fileService.requireFile(fileId);
        if (!ACCESS_PUBLIC.equalsIgnoreCase(file.getAccessLevel())) {
            throw new ServiceException(ErrorCodeConstants.FILE_ACCESS_DENIED);
        }
        String url = createPresignedGetUrl(file);
        return FileDownloadRedirect.builder()
                .url(url)
                .cacheControl(PUBLIC_CACHE_CONTROL)
                .build();
    }

    @Override
    public FileDownloadRedirect resolveAdminDownload(Long fileId) {
        InfraFileDO file = fileService.requireFile(fileId);
        String url = createPresignedGetUrl(file);
        return FileDownloadRedirect.builder()
                .url(url)
                .build();
    }

    private String createPresignedGetUrl(InfraFileDO file) {
        StorageProviderConfig providerConfig = storageProviderService.resolveProviderConfig(file.getProvider());
        ObjectStorageClient client = clientFactory.getClient(ObjectStorageProviderType.MINIO);
        Duration ttl = ACCESS_PUBLIC.equalsIgnoreCase(file.getAccessLevel())
                ? PUBLIC_PRESIGNED_TTL
                : PRIVATE_PRESIGNED_TTL;
        return client.createPresignedGet(providerConfig, file.getObjectKey(), ttl);
    }
}
