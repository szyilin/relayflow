package com.relayflow.module.infra.service.file;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.oss.core.ObjectStorageClient;
import com.relayflow.framework.oss.core.ObjectStorageClientFactory;
import com.relayflow.framework.oss.core.ObjectStorageProviderType;
import com.relayflow.framework.oss.core.model.PresignedUpload;
import com.relayflow.framework.oss.core.model.StorageObjectMeta;
import com.relayflow.framework.oss.core.model.StorageProviderConfig;
import com.relayflow.framework.tenant.config.TenantProperties;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.infra.controller.admin.file.vo.FileUploadConfirmReqVO;
import com.relayflow.module.infra.controller.admin.file.vo.FileUploadConfirmRespVO;
import com.relayflow.module.infra.controller.admin.file.vo.FileUploadSessionCreateReqVO;
import com.relayflow.module.infra.controller.admin.file.vo.FileUploadSessionRespVO;
import com.relayflow.module.infra.dal.dataobject.InfraFileDO;
import com.relayflow.module.infra.dal.dataobject.InfraFileUploadSessionDO;
import com.relayflow.module.infra.dal.mapper.InfraFileUploadSessionMapper;
import com.relayflow.module.infra.enums.ErrorCodeConstants;
import com.relayflow.module.infra.service.storage.StorageProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUploadSessionServiceImpl implements FileUploadSessionService {

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_CONFIRMED = "confirmed";
    private static final String STATUS_EXPIRED = "expired";
    private static final String MODE_PRESIGNED_PUT = "presigned_put";
    private static final String ACCESS_PUBLIC = "public";
    private static final String ACCESS_PRIVATE = "private";
    private static final Duration SESSION_TTL = Duration.ofMinutes(15);

    private final InfraFileUploadSessionMapper sessionMapper;
    private final FileService fileService;
    private final StorageProviderService storageProviderService;
    private final ObjectStorageClientFactory clientFactory;
    private final TenantProperties tenantProperties;

    @Override
    @Transactional
    public FileUploadSessionRespVO createSession(FileUploadSessionCreateReqVO request) {
        validateCreateRequest(request);

        Long tenantId = resolveTenantId();
        String provider = storageProviderService.resolveEffectiveProvider();
        StorageProviderConfig providerConfig = storageProviderService.resolveEffectiveProviderConfig();
        String objectKey = buildObjectKey(tenantId, request.getFilename());

        ObjectStorageClient client = clientFactory.getClient(ObjectStorageProviderType.MINIO);
        PresignedUpload presignedUpload = client.createPresignedPut(
                providerConfig,
                objectKey,
                request.getMimeType().trim(),
                SESSION_TTL);

        InfraFileUploadSessionDO session = new InfraFileUploadSessionDO();
        session.setTenantId(tenantId);
        session.setStatus(STATUS_PENDING);
        session.setProvider(provider);
        session.setObjectKey(objectKey);
        session.setOriginalName(request.getFilename().trim());
        session.setMimeType(request.getMimeType().trim());
        session.setSize(request.getSize());
        session.setAccessLevel(normalizeAccessLevel(request.getAccessLevel()));
        session.setExpiresAt(OffsetDateTime.ofInstant(presignedUpload.getExpiresAt(), ZoneOffset.UTC));
        sessionMapper.insert(session);

        FileUploadSessionRespVO response = new FileUploadSessionRespVO();
        response.setUploadId(session.getId());
        response.setMode(MODE_PRESIGNED_PUT);
        response.setObjectKey(objectKey);
        response.setUploadUrl(presignedUpload.getUploadUrl());
        response.setHeaders(presignedUpload.getHeaders());
        response.setExpiresAt(session.getExpiresAt());
        return response;
    }

    @Override
    @Transactional
    public FileUploadConfirmRespVO confirm(FileUploadConfirmReqVO request) {
        Long tenantId = resolveTenantId();
        InfraFileUploadSessionDO session = requirePendingSession(request.getUploadId(), tenantId);
        rejectIfExpired(session);

        if (!STATUS_PENDING.equals(session.getStatus())) {
            throw new ServiceException(ErrorCodeConstants.FILE_UPLOAD_SESSION_INVALID_STATUS);
        }

        StorageProviderConfig providerConfig = storageProviderService.resolveEffectiveProviderConfig();
        ObjectStorageClient client = clientFactory.getClient(ObjectStorageProviderType.MINIO);
        StorageObjectMeta objectMeta = client.headObject(providerConfig, session.getObjectKey());
        if (objectMeta == null) {
            throw new ServiceException(ErrorCodeConstants.FILE_UPLOAD_OBJECT_NOT_FOUND);
        }

        if (objectMeta.getSize() != request.getSize()) {
            throw new ServiceException(ErrorCodeConstants.FILE_UPLOAD_SIZE_MISMATCH);
        }
        if (StringUtils.hasText(request.getEtag())) {
            String requestEtag = normalizeEtag(request.getEtag());
            String objectEtag = normalizeEtag(objectMeta.getEtag());
            if (!requestEtag.equalsIgnoreCase(objectEtag)) {
                throw new ServiceException(ErrorCodeConstants.FILE_UPLOAD_INVALID_REQUEST);
            }
        }

        InfraFileDO file = fileService.createFromSession(session, providerConfig, objectMeta);

        session.setStatus(STATUS_CONFIRMED);
        sessionMapper.updateById(session);

        FileUploadConfirmRespVO response = new FileUploadConfirmRespVO();
        response.setFileId(file.getId());
        response.setStorageUri(file.getStorageUri());
        return response;
    }

    private void validateCreateRequest(FileUploadSessionCreateReqVO request) {
        if (request == null
                || !StringUtils.hasText(request.getFilename())
                || request.getSize() == null
                || request.getSize() <= 0
                || !StringUtils.hasText(request.getMimeType())) {
            throw new ServiceException(ErrorCodeConstants.FILE_UPLOAD_INVALID_REQUEST);
        }
        if (StringUtils.hasText(request.getAccessLevel())) {
            String accessLevel = request.getAccessLevel().trim().toLowerCase(Locale.ROOT);
            if (!ACCESS_PUBLIC.equals(accessLevel) && !ACCESS_PRIVATE.equals(accessLevel)) {
                throw new ServiceException(ErrorCodeConstants.FILE_UPLOAD_INVALID_REQUEST);
            }
        }
    }

    private InfraFileUploadSessionDO requirePendingSession(Long uploadId, Long tenantId) {
        InfraFileUploadSessionDO session = sessionMapper.selectOne(Wrappers.<InfraFileUploadSessionDO>lambdaQuery()
                .eq(InfraFileUploadSessionDO::getId, uploadId)
                .eq(InfraFileUploadSessionDO::getTenantId, tenantId)
                .last("LIMIT 1"));
        if (session == null) {
            throw new ServiceException(ErrorCodeConstants.FILE_UPLOAD_SESSION_NOT_FOUND);
        }
        return session;
    }

    private void rejectIfExpired(InfraFileUploadSessionDO session) {
        if (session.getExpiresAt() == null || !session.getExpiresAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
            if (STATUS_PENDING.equals(session.getStatus())) {
                session.setStatus(STATUS_EXPIRED);
                sessionMapper.updateById(session);
            }
            throw new ServiceException(ErrorCodeConstants.FILE_UPLOAD_SESSION_EXPIRED);
        }
    }

    private String normalizeAccessLevel(String accessLevel) {
        if (!StringUtils.hasText(accessLevel)) {
            return ACCESS_PRIVATE;
        }
        return accessLevel.trim().toLowerCase(Locale.ROOT);
    }

    static String buildObjectKey(Long tenantId, String filename) {
        String trimmedFilename = filename.trim();
        String extension = extractExtension(trimmedFilename);
        YearMonth yearMonth = YearMonth.now(ZoneOffset.UTC);
        String uuid = UUID.randomUUID().toString();
        return "tenant/" + tenantId + "/files/"
                + yearMonth.getYear() + "/"
                + String.format("%02d", yearMonth.getMonthValue()) + "/"
                + uuid + extension;
    }

    private static String extractExtension(String filename) {
        int slash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        String baseName = slash >= 0 ? filename.substring(slash + 1) : filename;
        int dot = baseName.lastIndexOf('.');
        if (dot <= 0 || dot == baseName.length() - 1) {
            return "";
        }
        return baseName.substring(dot);
    }

    private static String normalizeEtag(String etag) {
        if (etag == null) {
            return "";
        }
        String trimmed = etag.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContextHolder.get();
        return tenantId != null ? tenantId : tenantProperties.getDefaultId();
    }
}
