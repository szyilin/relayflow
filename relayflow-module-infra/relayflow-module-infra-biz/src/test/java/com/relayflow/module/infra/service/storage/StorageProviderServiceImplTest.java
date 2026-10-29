package com.relayflow.module.infra.service.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.common.encrypt.AesGcmEncryptor;
import com.relayflow.framework.oss.config.StorageProperties;
import com.relayflow.framework.oss.core.ObjectStorageClient;
import com.relayflow.framework.oss.core.ObjectStorageClientFactory;
import com.relayflow.framework.oss.core.ObjectStorageProviderType;
import com.relayflow.framework.oss.core.model.StorageProviderConfig;
import com.relayflow.framework.tenant.config.TenantProperties;
import com.relayflow.module.infra.controller.admin.storage.vo.StorageTestConnectionReqVO;
import com.relayflow.module.infra.dal.dataobject.InfraStorageProviderDO;
import com.relayflow.module.infra.dal.mapper.InfraFileMapper;
import com.relayflow.module.infra.dal.mapper.InfraStorageProviderMapper;
import com.relayflow.module.infra.service.storage.model.StorageProviderConfigJson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageProviderServiceImplTest {
    @Mock private InfraStorageProviderMapper providerMapper;
    @Mock private InfraFileMapper fileMapper;
    @Mock private ObjectStorageClientFactory clientFactory;
    @Mock private ObjectStorageClient client;

    private final AesGcmEncryptor encryptor = new AesGcmEncryptor(new byte[32]);
    private final ObjectMapper mapper = new ObjectMapper();
    private StorageProperties properties;
    private StorageProviderServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = MinioAddressingTest.managedProperties("http://minio:9000", "http://localhost:19000");
        properties.getMinio().setAccessKey("bootstrap-access");
        properties.getMinio().setSecretKey("bootstrap-secret");
        properties.getMinio().setBucket("bootstrap-bucket");
        service = new StorageProviderServiceImpl(providerMapper, fileMapper, clientFactory, properties,
                encryptor, mapper, new TenantProperties(), mock(StorageLocationService.class), mock(com.relayflow.module.infra.dal.mapper.StorageRevisionExtMapper.class));
    }

    @Test
    void bootstrapTargetUsesDualAddresses() {
        when(providerMapper.selectList(any())).thenReturn(List.of());
        assertManaged(service.resolveEffectiveProviderConfig(), "bootstrap-bucket", "bootstrap-access");
        verify(providerMapper, never()).insert(any(InfraStorageProviderDO.class));
    }

    @Test
    void tenantTargetMapsBothManagedOriginsWithoutRewritingJson() throws Exception {
        for (String endpoint : List.of("http://MINIO:9000/", "http://localhost:19000/")) {
            InfraStorageProviderDO row = tenantRow(endpoint);
            String originalJson = row.getConfigJson();
            when(providerMapper.selectList(any())).thenReturn(List.of(row));
            when(providerMapper.selectOne(any())).thenReturn(row);
            assertManaged(service.resolveEffectiveProviderConfig(), "tenant-bucket", "tenant-access");
            assertEquals(originalJson, row.getConfigJson());
        }
        verify(providerMapper, never()).updateById(any(InfraStorageProviderDO.class));
    }

    @Test
    void independentTenantStorageKeepsItsOwnEndpointAndCredentials() throws Exception {
        InfraStorageProviderDO row = tenantRow("https://independent.example");
        when(providerMapper.selectList(any())).thenReturn(List.of(row));
        when(providerMapper.selectOne(any())).thenReturn(row);
        for (StorageProviderConfig config : List.of(service.resolveEffectiveProviderConfig())) {
            assertEquals("https://independent.example", config.getEndpoint());
            assertNull(config.getPublicEndpoint());
            assertNull(config.getRegion());
            assertEquals("tenant-secret", config.getSecretKey());
        }
    }

    @Test
    void inlineAndSavedConnectionTestsUseManagedInternalOrigin() throws Exception {
        when(clientFactory.getClient(ObjectStorageProviderType.MINIO)).thenReturn(client);
        StorageTestConnectionReqVO request = new StorageTestConnectionReqVO();
        request.setSource("tenant");
        request.setEndpoint("http://localhost:19000");
        request.setBucket("inline-bucket");
        request.setAccessKey("inline-access");
        request.setSecretKey("inline-secret");
        service.testConnection(request);

        InfraStorageProviderDO row = tenantRow("http://localhost:19000");
        when(providerMapper.selectOne(any())).thenReturn(row);
        StorageTestConnectionReqVO savedRequest = new StorageTestConnectionReqVO();
        savedRequest.setSource("tenant");
        savedRequest.setProvider("minio");
        service.testConnection(savedRequest);

        ArgumentCaptor<StorageProviderConfig> configs = ArgumentCaptor.forClass(StorageProviderConfig.class);
        verify(client, times(2)).checkConnectivity(configs.capture());
        assertManaged(configs.getAllValues().get(0), "inline-bucket", "inline-access");
        assertManaged(configs.getAllValues().get(1), "tenant-bucket", "tenant-access");
    }

    private InfraStorageProviderDO tenantRow(String endpoint) throws Exception {
        StorageProviderConfigJson json = new StorageProviderConfigJson();
        json.setEndpoint(endpoint);
        json.setBucket("tenant-bucket");
        json.setAccessKey("tenant-access");
        json.setSecretKeyEnc(encryptor.encrypt("tenant-secret"));
        json.setPathPrefix("prefix/");
        InfraStorageProviderDO row = new InfraStorageProviderDO();
        row.setIsDefault(1);
        row.setConfigJson(mapper.writeValueAsString(json));
        return row;
    }

    private void assertManaged(StorageProviderConfig config, String bucket, String accessKey) {
        assertEquals("http://minio:9000", config.getEndpoint());
        assertEquals("http://localhost:19000", config.getPublicEndpoint());
        assertEquals("us-east-1", config.getRegion());
        assertEquals(bucket, config.getBucket());
        assertEquals(accessKey, config.getAccessKey());
    }
}
