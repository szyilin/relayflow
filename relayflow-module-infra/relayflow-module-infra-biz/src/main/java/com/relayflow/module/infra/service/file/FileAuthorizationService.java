package com.relayflow.module.infra.service.file;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.MemberIdentityProvider;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.infra.api.file.FileAccessPolicy;
import com.relayflow.module.infra.dal.dataobject.InfraFileDO;
import com.relayflow.module.infra.dal.dataobject.InfraFileBindingDO;
import com.relayflow.module.infra.dal.mapper.InfraFileBindingMapper;
import com.relayflow.module.infra.enums.ErrorCodeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FileAuthorizationService {
    private final FileService files;
    private final InfraFileBindingMapper bindings;
    private final ObjectProvider<FileAccessPolicy> policies;
    private final MemberIdentityProvider members;

    public InfraFileDO requireReadable(Long tenantId, Long userId, Long fileId) {
        members.requireActiveVersion(tenantId, userId);
        if (!Objects.equals(tenantId, TenantContextHolder.get())) {
            throw new ServiceException(ErrorCodeConstants.FILE_ACCESS_DENIED);
        }
        InfraFileDO file = files.requireFile(fileId);
        if (!Objects.equals(tenantId, file.getTenantId())) {
            throw new ServiceException(ErrorCodeConstants.FILE_NOT_FOUND);
        }
        if ("public".equals(file.getAccessLevel())) return file;
        if (!"private".equals(file.getAccessLevel())) throw denied();
        var refs = bindings.selectList(Wrappers.<InfraFileBindingDO>lambdaQuery()
                .eq(InfraFileBindingDO::getTenantId, tenantId).eq(InfraFileBindingDO::getFileId, fileId));
        if (refs.isEmpty() && userId != null && Objects.equals(userId, file.getCreator())) return file;
        for (var ref : refs) {
            for (var policy : policies.orderedStream().toList()) {
                if (!policy.bizType().equals(ref.getBizType())) continue;
                try {
                    if (policy.canRead(tenantId, userId, fileId, ref.getBizId())) return file;
                } catch (RuntimeException ex) {
                    // This reference is unverifiable; another independent valid reference may still grant access.
                }
            }
        }
        throw denied();
    }
    private ServiceException denied() { return new ServiceException(ErrorCodeConstants.FILE_ACCESS_DENIED); }
}
