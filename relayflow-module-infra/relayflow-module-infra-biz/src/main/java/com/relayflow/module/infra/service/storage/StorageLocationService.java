package com.relayflow.module.infra.service.storage;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.common.encrypt.AesGcmEncryptor;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.oss.config.StorageProperties;
import com.relayflow.framework.oss.core.*;
import com.relayflow.framework.oss.core.model.StorageProviderConfig;
import com.relayflow.module.infra.controller.admin.storage.vo.StorageRevisionRespVO;
import com.relayflow.module.infra.dal.dataobject.InfraStorageConfigRevisionDO;
import com.relayflow.module.infra.dal.mapper.StorageRevisionExtMapper;
import com.relayflow.module.infra.enums.ErrorCodeConstants;
import com.relayflow.module.infra.service.storage.model.StorageProviderConfigJson;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StorageLocationService {
    private final StorageRevisionExtMapper mapper;
    private final StorageProperties properties;
    private final AesGcmEncryptor encryption;
    private final ObjectMapper json;
    private final ObjectStorageClientFactory clients;

    public record Pinned(Long revisionId, StorageProviderConfig config, String pathPrefix) {}

    @Transactional
    public InfraStorageConfigRevisionDO ensure(Long tenant, String source, StorageProviderConfig input, boolean legacyRelativeKeys) {
        return ensureInternal(tenant,source,input,legacyRelativeKeys,false);
    }
    @Transactional
    public InfraStorageConfigRevisionDO ensureBootstrap(Long tenant,StorageProviderConfig input) {
        return ensureInternal(tenant,"bootstrap",input,false,true);
    }
    private InfraStorageConfigRevisionDO ensureInternal(Long tenant, String source, StorageProviderConfig input, boolean legacyRelativeKeys, boolean useStoredCredentials) {
        if (tenant==null || tenant<=0 || !("bootstrap".equals(source)||"tenant".equals(source))
                || blank(input.getBucket())==null || blank(input.getAccessKey())==null || blank(input.getSecretKey())==null) throw invalid();
        var value = new StorageProviderConfigJson();
        value.setEndpoint(origin(input.getEndpoint())); value.setBucket(input.getBucket()); value.setRegion(blank(input.getRegion()));
        value.setPathPrefix(prefix(input.getPathPrefix())); value.setLegacyRelativeKeys(legacyRelativeKeys);
        value.setManaged(value.getEndpoint().equals(origin(properties.getMinio().getEndpoint()))
                || (properties.getMinio().getPublicEndpoint()!=null && !properties.getMinio().getPublicEndpoint().isBlank() && value.getEndpoint().equals(origin(properties.getMinio().getPublicEndpoint()))));
        if (value.isManaged()) {
            value.setEndpoint(origin(properties.getMinio().getEndpoint()));
            if(value.getRegion()==null) value.setRegion(blank(properties.getMinio().getRegion()));
        }
        value.setUseSsl(input.isUseSsl());
        value.setAccessKey(input.getAccessKey()); value.setSecretKeyEnc(encryption.encrypt(input.getSecretKey()));
        String key=locationKey(source,value);
        var existing=mapper.findKey(tenant,key);
        if(existing==null) {
            var row=new InfraStorageConfigRevisionDO(); row.setId(IdWorker.getId()); row.setTenantId(tenant);
            row.setProvider("minio");row.setSource(source);row.setLocationKey(key);row.setConfigJson(write(value));
            mapper.insertIfMissing(row); existing=mapper.findKey(tenant,key);
        }
        var saved=parse(existing.getConfigJson());
        if(!useStoredCredentials && (!Objects.equals(saved.getAccessKey(),input.getAccessKey()) || !Objects.equals(encryption.decrypt(saved.getSecretKeyEnc()),input.getSecretKey())))
            throw new ServiceException(ErrorCodeConstants.STORAGE_CREDENTIAL_ROTATION_REQUIRED);
        return existing;
    }
    public InfraStorageConfigRevisionDO require(Long tenant,Long id) {
        if(id==null) throw unresolved();
        var row=mapper.find(tenant,id); if(row==null) throw unresolved(); return row;
    }
    public Pinned pin(Long tenant,Long id) {
        var row=require(tenant,id);var value=parse(row.getConfigJson());
        if(value.isLegacyRelativeKeys()) throw invalid();
        return new Pinned(id,runtime(row),prefix(value.getPathPrefix()));
    }
    public StorageProviderConfig resolve(Long tenant,Long id) { return runtime(require(tenant,id)); }
    public StorageProviderConfig runtime(InfraStorageConfigRevisionDO row) {
        var value=parse(row.getConfigJson());
        String endpoint=value.getEndpoint(), publicEndpoint=null, region=value.getRegion();
        if(value.isManaged() && origin(endpoint).equals(origin(properties.getMinio().getEndpoint()))) {
            endpoint=properties.getMinio().getEndpoint();publicEndpoint=blank(properties.getMinio().getPublicEndpoint());
            if(region==null) region=blank(properties.getMinio().getRegion());
        }
        return StorageProviderConfig.builder().providerType(ObjectStorageProviderType.MINIO).endpoint(endpoint)
                .publicEndpoint(publicEndpoint).region(region).bucket(value.getBucket()).accessKey(value.getAccessKey())
                .secretKey(encryption.decrypt(value.getSecretKeyEnc())).useSsl(Boolean.TRUE.equals(value.getUseSsl()))
                .pathPrefix(value.isLegacyRelativeKeys()?prefix(value.getPathPrefix()):"").build();
    }
    @Transactional
    public void rotate(Long tenant,Long id,String accessKey,String secret) {
        if(blank(accessKey)==null || blank(secret)==null) throw invalid();
        var config=resolve(tenant,id).toBuilder().accessKey(accessKey.trim()).secretKey(secret.trim()).build();
        try { clients.getClient(ObjectStorageProviderType.MINIO).checkConnectivity(config); }
        catch(RuntimeException ex) { throw new ServiceException(ErrorCodeConstants.STORAGE_TEST_CONNECTION_FAILED); }
        mapper.credentials(tenant,id,accessKey.trim(),encryption.encrypt(secret.trim()));
    }
    public boolean referenced(Long tenant,Long id) { return id!=null && (mapper.files(tenant,id)>0 || mapper.sessions(tenant,id)>0); }
    public List<StorageRevisionRespVO> list(Long tenant) {
        return mapper.list(tenant).stream().map(row->{
            var value=parse(row.getConfigJson());var view=new StorageRevisionRespVO();view.setId(row.getId());view.setSource(row.getSource());
            view.setEndpoint(value.getEndpoint());view.setBucket(value.getBucket());view.setRegion(value.getRegion());view.setPathPrefix(value.getPathPrefix());
            view.setLegacyRelativeKeys(value.isLegacyRelativeKeys());view.setFileReferences(mapper.files(tenant,row.getId()));view.setSessionReferences(mapper.sessions(tenant,row.getId()));return view;
        }).toList();
    }
    public StorageProviderConfigJson parse(String value) {
        try { return json.readValue(value,StorageProviderConfigJson.class); }
        catch(JsonProcessingException ex) { throw invalid(); }
    }
    public String write(StorageProviderConfigJson value) {
        try { return json.writeValueAsString(value); } catch(JsonProcessingException ex) { throw invalid(); }
    }
    public String locationKey(String source,StorageProviderConfigJson value) {
        return hash(String.join("|",source,origin(value.getEndpoint()),Objects.toString(value.getBucket(),""),
                Objects.toString(blank(value.getRegion()),""),prefix(value.getPathPrefix()),Boolean.toString(value.isLegacyRelativeKeys())));
    }
    public static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(); }
    }
    public static String origin(String value) {
        try {
            var uri=URI.create(value.trim());String scheme=uri.getScheme().toLowerCase(Locale.ROOT);
            if(!Set.of("http","https").contains(scheme)||uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null
                    || (uri.getPath()!=null&&!uri.getPath().isBlank()&&!uri.getPath().equals("/"))) throw invalid();
            int port=uri.getPort()<0?(scheme.equals("https")?443:80):uri.getPort();if(port<1||port>65535)throw invalid();
            return scheme+"://"+uri.getHost().toLowerCase(Locale.ROOT)+":"+port;
        }catch(IllegalArgumentException|NullPointerException ex) { throw invalid(); }
    }
    public static String prefix(String value) {
        if(value==null||value.isBlank())return "";String result=value.trim();while(result.startsWith("/"))result=result.substring(1);
        return result.isEmpty()||result.endsWith("/")?result:result+"/";
    }
    private static String blank(String value) { return value==null||value.isBlank()?null:value.trim(); }
    private static ServiceException invalid() { return new ServiceException(ErrorCodeConstants.STORAGE_PROVIDER_CONFIG_INVALID); }
    private static ServiceException unresolved() { return new ServiceException(ErrorCodeConstants.STORAGE_LOCATION_UNRESOLVED); }
}
