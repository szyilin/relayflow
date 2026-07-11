package com.relayflow.module.infra.service.storage;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.common.encrypt.AesGcmEncryptor;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.oss.config.StorageProperties;
import com.relayflow.framework.oss.core.ObjectStorageClient;
import com.relayflow.framework.oss.core.ObjectStorageClientFactory;
import com.relayflow.framework.oss.core.ObjectStorageProviderType;
import com.relayflow.framework.oss.core.model.StorageProviderConfig;
import com.relayflow.framework.tenant.config.TenantProperties;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.infra.controller.admin.storage.vo.StorageBootstrapSummaryVO;
import com.relayflow.module.infra.controller.admin.storage.vo.StorageConfigRespVO;
import com.relayflow.module.infra.controller.admin.storage.vo.StorageEffectiveSourceReqVO;
import com.relayflow.module.infra.controller.admin.storage.vo.StorageProviderRespVO;
import com.relayflow.module.infra.controller.admin.storage.vo.StorageProviderSaveReqVO;
import com.relayflow.module.infra.controller.admin.storage.vo.StorageTestConnectionReqVO;
import com.relayflow.module.infra.convert.StorageProviderConvert;
import com.relayflow.module.infra.dal.dataobject.InfraFileDO;
import com.relayflow.module.infra.dal.dataobject.InfraStorageProviderDO;
import com.relayflow.module.infra.dal.mysql.InfraFileMapper;
import com.relayflow.module.infra.dal.mysql.InfraStorageProviderMapper;
import com.relayflow.module.infra.enums.ErrorCodeConstants;
import com.relayflow.module.infra.service.storage.model.StorageProviderConfigJson;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StorageProviderServiceImpl implements StorageProviderService {

    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_LEGACY = "legacy";
    private static final String PROVIDER_MINIO = "minio";
    private static final String SOURCE_BOOTSTRAP = "bootstrap";
    private static final String SOURCE_TENANT = "tenant";

    private final InfraStorageProviderMapper providerMapper;
    private final InfraFileMapper fileMapper;
    private final ObjectStorageClientFactory clientFactory;
    private final StorageProperties storageProperties;
    private final AesGcmEncryptor encryptor;
    private final ObjectMapper objectMapper;
    private final TenantProperties tenantProperties;

    @Override
    public StorageConfigRespVO getConfig() {
        Long tenantId = resolveTenantId();
        List<InfraStorageProviderDO> rows = providerMapper.selectList(Wrappers.<InfraStorageProviderDO>lambdaQuery()
                .eq(InfraStorageProviderDO::getTenantId, tenantId)
                .orderByDesc(InfraStorageProviderDO::getIsDefault)
                .orderByAsc(InfraStorageProviderDO::getProvider));
        List<StorageProviderRespVO> providers = rows.stream()
                .map(row -> StorageProviderConvert.toVo(row, parseConfigJson(row.getConfigJson())))
                .toList();
        StorageConfigRespVO response = new StorageConfigRespVO();
        response.setBootstrap(buildBootstrapSummary());
        response.setEffectiveSource(resolveEffectiveSource(rows));
        response.setProviders(providers);
        return response;
    }

    @Override
    @Transactional
    public void setEffectiveSource(StorageEffectiveSourceReqVO request) {
        String source = normalizeSource(request.getSource());
        Long tenantId = resolveTenantId();
        if (SOURCE_BOOTSTRAP.equals(source)) {
            clearDefaultFlags(tenantId);
            return;
        }

        InfraStorageProviderDO tenantProvider = findProvider(tenantId, PROVIDER_MINIO);
        if (tenantProvider == null) {
            throw new ServiceException(ErrorCodeConstants.STORAGE_PROVIDER_NOT_FOUND);
        }
        clearDefaultFlags(tenantId);
        tenantProvider.setIsDefault(1);
        tenantProvider.setStatus(STATUS_ACTIVE);
        providerMapper.updateById(tenantProvider);
    }

    @Override
    @Transactional
    public void saveConfig(StorageProviderSaveReqVO request) {
        String provider = normalizeProvider(request.getProvider());
        validateSupportedProvider(provider);
        Long tenantId = resolveTenantId();

        InfraStorageProviderDO existing = findProvider(tenantId, provider);
        StorageProviderConfigJson configJson = existing != null
                ? parseConfigJson(existing.getConfigJson())
                : new StorageProviderConfigJson();

        configJson.setEndpoint(request.getEndpoint().trim());
        configJson.setBucket(request.getBucket().trim());
        configJson.setAccessKey(request.getAccessKey().trim());
        configJson.setUseSsl(Boolean.TRUE.equals(request.getUseSsl()));
        configJson.setPathPrefix(normalizePathPrefix(request.getPathPrefix()));

        if (StringUtils.hasText(request.getSecretKey())) {
            configJson.setSecretKeyEnc(encryptor.encrypt(request.getSecretKey().trim()));
        } else if (existing == null || !StringUtils.hasText(configJson.getSecretKeyEnc())) {
            throw new ServiceException(ErrorCodeConstants.STORAGE_PROVIDER_CONFIG_INVALID);
        }

        List<InfraStorageProviderDO> tenantRows = providerMapper.selectList(Wrappers.<InfraStorageProviderDO>lambdaQuery()
                .eq(InfraStorageProviderDO::getTenantId, tenantId));
        boolean tenantEffective = SOURCE_TENANT.equals(resolveEffectiveSource(tenantRows));

        InfraStorageProviderDO row = existing != null ? existing : new InfraStorageProviderDO();
        row.setTenantId(tenantId);
        row.setProvider(provider);
        row.setStatus(STATUS_ACTIVE);
        row.setIsDefault(tenantEffective ? 1 : 0);
        row.setConfigJson(writeConfigJson(configJson));

        if (existing == null) {
            providerMapper.insert(row);
        } else {
            providerMapper.updateById(row);
        }
    }

    @Override
    @Transactional
    public void deleteConfig(String provider) {
        String normalizedProvider = normalizeProvider(provider);
        validateSupportedProvider(normalizedProvider);
        Long tenantId = resolveTenantId();
        InfraStorageProviderDO existing = requireProvider(tenantId, normalizedProvider);

        Long referenceCount = fileMapper.selectCount(Wrappers.<InfraFileDO>lambdaQuery()
                .eq(InfraFileDO::getTenantId, tenantId)
                .eq(InfraFileDO::getProvider, normalizedProvider));
        if (referenceCount != null && referenceCount > 0) {
            throw new ServiceException(ErrorCodeConstants.STORAGE_PROVIDER_REFERENCED);
        }

        providerMapper.deleteById(existing.getId());
    }

    @Override
    public void testConnection(StorageTestConnectionReqVO request) {
        if (SOURCE_BOOTSTRAP.equalsIgnoreCase(request.getSource())) {
            runConnectivityCheck(storageProperties.toBootstrapConfig());
            return;
        }

        String provider = normalizeProvider(request.getProvider());
        if (!StringUtils.hasText(provider)) {
            provider = PROVIDER_MINIO;
        }
        validateSupportedProvider(provider);
        StorageProviderConfig runtimeConfig = buildRuntimeConfig(resolveTenantId(), provider, request);
        runConnectivityCheck(runtimeConfig);
    }

    private StorageBootstrapSummaryVO buildBootstrapSummary() {
        StorageBootstrapSummaryVO summary = new StorageBootstrapSummaryVO();
        ObjectStorageProviderType defaultProvider = storageProperties.getDefaultProvider();
        if (defaultProvider == null) {
            summary.setAvailable(false);
            return summary;
        }
        summary.setAvailable(true);
        summary.setProvider(defaultProvider.name().toLowerCase());
        try {
            storageProperties.validateBootstrapBlocks();
            summary.setCredentialsConfigured(true);
        } catch (IllegalStateException ex) {
            summary.setCredentialsConfigured(false);
        }
        return summary;
    }

    private String resolveEffectiveSource(List<InfraStorageProviderDO> rows) {
        boolean tenantDefault = rows.stream().anyMatch(row -> Integer.valueOf(1).equals(row.getIsDefault()));
        return tenantDefault ? SOURCE_TENANT : SOURCE_BOOTSTRAP;
    }

    private String normalizeSource(String source) {
        if (!StringUtils.hasText(source)) {
            throw new ServiceException(ErrorCodeConstants.STORAGE_PROVIDER_CONFIG_INVALID);
        }
        String normalized = source.trim().toLowerCase();
        if (!SOURCE_BOOTSTRAP.equals(normalized) && !SOURCE_TENANT.equals(normalized)) {
            throw new ServiceException(ErrorCodeConstants.STORAGE_PROVIDER_CONFIG_INVALID);
        }
        return normalized;
    }

    private StorageProviderConfig buildRuntimeConfig(Long tenantId,
                                                     String provider,
                                                     StorageTestConnectionReqVO request) {
        if (hasInlineConnectionParams(request)) {
            if (!StringUtils.hasText(request.getSecretKey())) {
                throw new ServiceException(ErrorCodeConstants.STORAGE_TEST_CONNECTION_FAILED);
            }
            return StorageProviderConfig.builder()
                    .providerType(ObjectStorageProviderType.MINIO)
                    .endpoint(request.getEndpoint().trim())
                    .accessKey(request.getAccessKey().trim())
                    .secretKey(request.getSecretKey().trim())
                    .bucket(request.getBucket().trim())
                    .useSsl(Boolean.TRUE.equals(request.getUseSsl()))
                    .pathPrefix(normalizePathPrefix(request.getPathPrefix()))
                    .build();
        }

        InfraStorageProviderDO saved = requireProvider(tenantId, provider);
        StorageProviderConfigJson configJson = parseConfigJson(saved.getConfigJson());
        if (!StringUtils.hasText(configJson.getSecretKeyEnc())) {
            throw new ServiceException(ErrorCodeConstants.STORAGE_TEST_CONNECTION_FAILED);
        }
        return toRuntimeConfig(configJson, encryptor.decrypt(configJson.getSecretKeyEnc()));
    }

    private void runConnectivityCheck(StorageProviderConfig config) {
        try {
            ObjectStorageClient client = clientFactory.getClient(ObjectStorageProviderType.MINIO);
            client.checkConnectivity(config);
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException(ErrorCodeConstants.STORAGE_TEST_CONNECTION_FAILED);
        }
    }

    private void markPreviousDefaultLegacy(Long tenantId, String newDefaultProvider) {
        InfraStorageProviderDO previousDefault = providerMapper.selectOne(Wrappers.<InfraStorageProviderDO>lambdaQuery()
                .eq(InfraStorageProviderDO::getTenantId, tenantId)
                .eq(InfraStorageProviderDO::getIsDefault, 1)
                .ne(InfraStorageProviderDO::getProvider, newDefaultProvider)
                .last("LIMIT 1"));
        if (previousDefault != null) {
            previousDefault.setIsDefault(0);
            previousDefault.setStatus(STATUS_LEGACY);
            providerMapper.updateById(previousDefault);
        }
    }

    private void clearDefaultFlags(Long tenantId) {
        InfraStorageProviderDO patch = new InfraStorageProviderDO();
        patch.setIsDefault(0);
        providerMapper.update(patch, Wrappers.<InfraStorageProviderDO>lambdaUpdate()
                .eq(InfraStorageProviderDO::getTenantId, tenantId)
                .eq(InfraStorageProviderDO::getIsDefault, 1));
    }

    private InfraStorageProviderDO findProvider(Long tenantId, String provider) {
        return providerMapper.selectOne(Wrappers.<InfraStorageProviderDO>lambdaQuery()
                .eq(InfraStorageProviderDO::getTenantId, tenantId)
                .eq(InfraStorageProviderDO::getProvider, provider)
                .last("LIMIT 1"));
    }

    private InfraStorageProviderDO requireProvider(Long tenantId, String provider) {
        InfraStorageProviderDO row = findProvider(tenantId, provider);
        if (row == null) {
            throw new ServiceException(ErrorCodeConstants.STORAGE_PROVIDER_NOT_FOUND);
        }
        return row;
    }

    private StorageProviderConfig toRuntimeConfig(StorageProviderConfigJson configJson, String plainSecret) {
        return StorageProviderConfig.builder()
                .providerType(ObjectStorageProviderType.MINIO)
                .endpoint(configJson.getEndpoint())
                .accessKey(configJson.getAccessKey())
                .secretKey(plainSecret)
                .bucket(configJson.getBucket())
                .useSsl(Boolean.TRUE.equals(configJson.getUseSsl()))
                .pathPrefix(normalizePathPrefix(configJson.getPathPrefix()))
                .build();
    }

    private boolean hasInlineConnectionParams(StorageTestConnectionReqVO request) {
        return StringUtils.hasText(request.getEndpoint())
                || StringUtils.hasText(request.getBucket())
                || StringUtils.hasText(request.getAccessKey());
    }

    private StorageProviderConfigJson parseConfigJson(String configJson) {
        if (!StringUtils.hasText(configJson)) {
            return new StorageProviderConfigJson();
        }
        try {
            return objectMapper.readValue(configJson, StorageProviderConfigJson.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Invalid storage provider config_json", ex);
        }
    }

    private String writeConfigJson(StorageProviderConfigJson configJson) {
        try {
            return objectMapper.writeValueAsString(configJson);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize storage provider config_json", ex);
        }
    }

    private void validateSupportedProvider(String provider) {
        if (!PROVIDER_MINIO.equals(provider)) {
            throw new ServiceException(ErrorCodeConstants.STORAGE_PROVIDER_UNSUPPORTED);
        }
    }

    private String normalizeProvider(String provider) {
        return provider != null ? provider.trim().toLowerCase() : "";
    }

    private String normalizePathPrefix(String pathPrefix) {
        if (!StringUtils.hasText(pathPrefix)) {
            return "";
        }
        String normalized = pathPrefix.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized.isEmpty() || normalized.endsWith("/") ? normalized : normalized + "/";
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContextHolder.get();
        return tenantId != null ? tenantId : tenantProperties.getDefaultId();
    }
}
