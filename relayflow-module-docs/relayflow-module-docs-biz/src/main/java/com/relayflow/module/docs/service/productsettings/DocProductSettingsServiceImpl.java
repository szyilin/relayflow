package com.relayflow.module.docs.service.productsettings;

import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.docs.controller.admin.vo.DocProductSettingsRespVO;
import com.relayflow.module.docs.controller.app.vo.DocEffectiveSettingsRespVO;
import com.relayflow.module.docs.dal.dataobject.DocTenantSettingsDO;
import com.relayflow.module.docs.dal.mapper.DocTenantSettingsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class DocProductSettingsServiceImpl implements DocProductSettingsService {

    private static final boolean DEFAULT_MEMBER_SHARE_ENABLED = true;
    private static final boolean DEFAULT_MARKDOWN_EXPORT_ENABLED = true;
    private static final boolean DEFAULT_DRIVE_UPLOAD_ENABLED = true;

    private final DocTenantSettingsMapper tenantSettingsMapper;

    @Override
    public DocProductSettingsRespVO getSettings() {
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        return toResp(findSettings(tenantId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocProductSettingsRespVO updateSettings(DocProductSettingsRespVO request) {
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        OffsetDateTime now = OffsetDateTime.now();
        DocTenantSettingsDO row = findSettings(tenantId);
        if (row == null) {
            row = new DocTenantSettingsDO();
            row.setTenantId(tenantId);
            row.setMemberShareEnabled(Boolean.TRUE.equals(request.getMemberShareEnabled()));
            row.setMarkdownExportEnabled(Boolean.TRUE.equals(request.getMarkdownExportEnabled()));
            row.setDriveUploadEnabled(Boolean.TRUE.equals(request.getDriveUploadEnabled()));
            row.setCreator(userId);
            row.setCreateTime(now);
            row.setUpdater(userId);
            row.setUpdateTime(now);
            tenantSettingsMapper.insert(row);
        } else {
            row.setMemberShareEnabled(Boolean.TRUE.equals(request.getMemberShareEnabled()));
            row.setMarkdownExportEnabled(Boolean.TRUE.equals(request.getMarkdownExportEnabled()));
            row.setDriveUploadEnabled(Boolean.TRUE.equals(request.getDriveUploadEnabled()));
            row.setUpdater(userId);
            row.setUpdateTime(now);
            tenantSettingsMapper.updateById(row);
        }
        return toResp(row);
    }

    @Override
    public DocEffectiveSettingsRespVO getEffectiveSettings() {
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        DocTenantSettingsDO row = findSettings(tenantId);
        DocEffectiveSettingsRespVO vo = new DocEffectiveSettingsRespVO();
        vo.setMemberShareEnabled(resolveMemberShareEnabled(row));
        vo.setMarkdownExportEnabled(resolveMarkdownExportEnabled(row));
        vo.setDriveUploadEnabled(resolveDriveUploadEnabled(row));
        return vo;
    }

    @Override
    public boolean resolveMemberShareEnabled(Long tenantId) {
        return resolveMemberShareEnabled(findSettings(tenantId));
    }

    @Override
    public boolean resolveMarkdownExportEnabled(Long tenantId) {
        return resolveMarkdownExportEnabled(findSettings(tenantId));
    }

    @Override
    public boolean resolveDriveUploadEnabled(Long tenantId) {
        return resolveDriveUploadEnabled(findSettings(tenantId));
    }

    private DocTenantSettingsDO findSettings(Long tenantId) {
        return tenantSettingsMapper.selectById(tenantId);
    }

    private boolean resolveMemberShareEnabled(DocTenantSettingsDO row) {
        if (row == null || row.getMemberShareEnabled() == null) {
            return DEFAULT_MEMBER_SHARE_ENABLED;
        }
        return Boolean.TRUE.equals(row.getMemberShareEnabled());
    }

    private boolean resolveMarkdownExportEnabled(DocTenantSettingsDO row) {
        if (row == null || row.getMarkdownExportEnabled() == null) {
            return DEFAULT_MARKDOWN_EXPORT_ENABLED;
        }
        return Boolean.TRUE.equals(row.getMarkdownExportEnabled());
    }

    private boolean resolveDriveUploadEnabled(DocTenantSettingsDO row) {
        if (row == null || row.getDriveUploadEnabled() == null) {
            return DEFAULT_DRIVE_UPLOAD_ENABLED;
        }
        return Boolean.TRUE.equals(row.getDriveUploadEnabled());
    }

    private DocProductSettingsRespVO toResp(DocTenantSettingsDO row) {
        DocProductSettingsRespVO vo = new DocProductSettingsRespVO();
        vo.setMemberShareEnabled(resolveMemberShareEnabled(row));
        vo.setMarkdownExportEnabled(resolveMarkdownExportEnabled(row));
        vo.setDriveUploadEnabled(resolveDriveUploadEnabled(row));
        return vo;
    }
}
