package com.relayflow.module.infra.service.file;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.pojo.PageResult;
import com.relayflow.framework.oss.core.model.StorageObjectMeta;
import com.relayflow.framework.oss.core.model.StorageProviderConfig;
import com.relayflow.framework.tenant.config.TenantProperties;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.infra.api.file.dto.FileBindReqDTO;
import com.relayflow.module.infra.api.file.dto.FileRespDTO;
import com.relayflow.module.infra.controller.admin.file.vo.FileListItemRespVO;
import com.relayflow.module.infra.controller.admin.file.vo.FilePageReqVO;
import com.relayflow.module.infra.convert.FileConvert;
import com.relayflow.module.infra.dal.dataobject.InfraFileBindingDO;
import com.relayflow.module.infra.dal.dataobject.InfraFileDO;
import com.relayflow.module.infra.dal.dataobject.InfraFileUploadSessionDO;
import com.relayflow.module.infra.dal.mapper.InfraFileBindingMapper;
import com.relayflow.module.infra.dal.mapper.InfraFileMapper;
import com.relayflow.module.infra.dal.mapper.InfraFilePublicMapper;
import com.relayflow.module.infra.enums.ErrorCodeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final String ACCESS_PUBLIC = "public";

    private final InfraFileMapper fileMapper;
    private final InfraFilePublicMapper filePublicMapper;
    private final InfraFileBindingMapper bindingMapper;
    private final TenantProperties tenantProperties;

    @Override
    public FileRespDTO getFile(Long fileId) {
        return FileConvert.INSTANCE.toDto(requireFile(fileId));
    }

    @Override
    public PageResult<FileListItemRespVO> getFilePage(FilePageReqVO request) {
        Long tenantId = resolveTenantId();
        Page<InfraFileDO> page = fileMapper.selectPage(
                new Page<>(request.getPageNo(), request.getPageSize()),
                Wrappers.<InfraFileDO>lambdaQuery()
                        .eq(InfraFileDO::getTenantId, tenantId)
                        .like(StringUtils.hasText(request.getKeyword()),
                                InfraFileDO::getOriginalName,
                                request.getKeyword() != null ? request.getKeyword().trim() : null)
                        .orderByDesc(InfraFileDO::getCreateTime));
        return PageResult.of(
                page.getRecords().stream().map(FileConvert.INSTANCE::toListItem).toList(),
                page.getTotal());
    }

    @Override
    public void deleteFile(Long fileId) {
        InfraFileDO file = requireFile(fileId);
        fileMapper.deleteById(file.getId());
    }

    @Override
    public InfraFileDO requireFile(Long fileId) {
        Long tenantId = resolveTenantId();
        InfraFileDO file = fileMapper.selectOne(Wrappers.<InfraFileDO>lambdaQuery()
                .eq(InfraFileDO::getId, fileId)
                .eq(InfraFileDO::getTenantId, tenantId)
                .last("LIMIT 1"));
        if (file == null) {
            throw new ServiceException(ErrorCodeConstants.FILE_NOT_FOUND);
        }
        return file;
    }

    @Override
    public InfraFileDO requirePublicFile(Long fileId) {
        InfraFileDO file = filePublicMapper.selectByIdGlobal(fileId);
        if (file == null) {
            throw new ServiceException(ErrorCodeConstants.FILE_NOT_FOUND);
        }
        if (!ACCESS_PUBLIC.equalsIgnoreCase(file.getAccessLevel())) {
            throw new ServiceException(ErrorCodeConstants.FILE_ACCESS_DENIED);
        }
        return file;
    }

    @Override
    public InfraFileDO createFromSession(InfraFileUploadSessionDO session,
                                         StorageProviderConfig providerConfig,
                                         StorageObjectMeta objectMeta) {
        InfraFileDO file = new InfraFileDO();
        file.setTenantId(session.getTenantId());
        file.setProvider(session.getProvider());
        file.setStorageUri(buildStorageUri(providerConfig.getBucket(), session.getObjectKey()));
        file.setObjectKey(session.getObjectKey());
        file.setOriginalName(session.getOriginalName());
        file.setMimeType(session.getMimeType());
        file.setSize(objectMeta.getSize());
        file.setAccessLevel(session.getAccessLevel());
        fileMapper.insert(file);
        return file;
    }

    @Override
    @Transactional
    public void bindFile(FileBindReqDTO request) {
        if (request == null
                || request.getFileId() == null
                || !StringUtils.hasText(request.getBizType())
                || request.getBizId() == null) {
            throw new ServiceException(ErrorCodeConstants.FILE_UPLOAD_INVALID_REQUEST);
        }

        Long tenantId = resolveTenantId();
        requireFile(request.getFileId());

        String bizType = request.getBizType().trim();
        Long existing = bindingMapper.selectCount(Wrappers.<InfraFileBindingDO>lambdaQuery()
                .eq(InfraFileBindingDO::getTenantId, tenantId)
                .eq(InfraFileBindingDO::getFileId, request.getFileId())
                .eq(InfraFileBindingDO::getBizType, bizType)
                .eq(InfraFileBindingDO::getBizId, request.getBizId()));
        if (existing != null && existing > 0) {
            return;
        }

        InfraFileBindingDO binding = new InfraFileBindingDO();
        binding.setTenantId(tenantId);
        binding.setFileId(request.getFileId());
        binding.setBizType(bizType);
        binding.setBizId(request.getBizId());
        bindingMapper.insert(binding);
    }

    static String buildStorageUri(String bucket, String objectKey) {
        return "minio://" + bucket + "/" + objectKey;
    }

    private Long resolveTenantId() {
        Long tenantId = TenantContextHolder.get();
        return tenantId != null ? tenantId : tenantProperties.getDefaultId();
    }
}
