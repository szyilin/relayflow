package com.relayflow.module.system.service.preference;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.system.controller.app.vo.AppUserPreferenceRespVO;
import com.relayflow.module.system.controller.app.vo.AppUserPreferenceUpdateReqVO;
import com.relayflow.module.system.dal.dataobject.SysUserPreferenceDO;
import com.relayflow.module.system.dal.mapper.SysUserPreferenceMapper;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserPreferenceServiceImpl implements UserPreferenceService {

    private final SysUserPreferenceMapper preferenceMapper;
    private final UserPreferenceDefaults preferenceDefaults;

    @Override
    public AppUserPreferenceRespVO getMyPreference() {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        SysUserPreferenceDO row = findRow(tenantId, userId);
        Map<String, Object> stored = row == null
                ? null
                : preferenceDefaults.parseSettingsJson(row.getSettings());
        return toResp(preferenceDefaults.merge(stored),
                row == null ? UserPreferenceDefaults.SCHEMA_VERSION : nullSafeVersion(row.getSchemaVersion()));
    }

    @Override
    @Transactional
    public AppUserPreferenceRespVO updateMyPreference(AppUserPreferenceUpdateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();

        StringBuilder error = new StringBuilder();
        Map<String, Object> sanitized = preferenceDefaults.sanitizeOrNull(request.getSettings(), error);
        if (sanitized == null) {
            throw new ServiceException(ErrorCodeConstants.USER_PREFERENCE_INVALID.getCode(),
                    error.length() > 0 ? error.toString() : ErrorCodeConstants.USER_PREFERENCE_INVALID.getMsg());
        }

        int version = request.getSchemaVersion() != null
                ? request.getSchemaVersion()
                : UserPreferenceDefaults.SCHEMA_VERSION;

        SysUserPreferenceDO row = findRow(tenantId, userId);
        String json = preferenceDefaults.toSettingsJson(sanitized);
        if (row == null) {
            row = new SysUserPreferenceDO();
            row.setTenantId(tenantId);
            row.setUserId(userId);
            row.setSettings(json);
            row.setSchemaVersion(version);
            preferenceMapper.insert(row);
        } else {
            row.setSettings(json);
            row.setSchemaVersion(version);
            preferenceMapper.updateById(row);
        }

        return toResp(sanitized, version);
    }

    private SysUserPreferenceDO findRow(Long tenantId, Long userId) {
        return preferenceMapper.selectOne(new LambdaQueryWrapper<SysUserPreferenceDO>()
                .eq(SysUserPreferenceDO::getTenantId, tenantId)
                .eq(SysUserPreferenceDO::getUserId, userId)
                .last("LIMIT 1"));
    }

    private int nullSafeVersion(Integer version) {
        return version == null ? UserPreferenceDefaults.SCHEMA_VERSION : version;
    }

    private AppUserPreferenceRespVO toResp(Map<String, Object> settings, int schemaVersion) {
        AppUserPreferenceRespVO resp = new AppUserPreferenceRespVO();
        resp.setSchemaVersion(schemaVersion);
        resp.setSettings(settings);
        return resp;
    }
}
