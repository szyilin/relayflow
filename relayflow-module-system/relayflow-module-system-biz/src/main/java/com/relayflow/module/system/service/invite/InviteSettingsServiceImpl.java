package com.relayflow.module.system.service.invite;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.system.controller.admin.invite.vo.InviteSettingsRespVO;
import com.relayflow.module.system.controller.admin.invite.vo.InviteSettingsUpdateReqVO;
import com.relayflow.module.system.dal.dataobject.SysTenantInviteSettingsDO;
import com.relayflow.module.system.dal.mapper.SysTenantInviteSettingsMapper;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InviteSettingsServiceImpl implements InviteSettingsService {

    static final String DEFAULT_INVITE_MODE = "ADMIN_ONLY";
    static final String DEFAULT_NON_ADMIN_SCOPE = "INVITER_DEPT";
    static final int DEFAULT_EXPIRE_DAYS = 3;

    private static final Set<String> INVITE_MODES = Set.of("OFF", "ADMIN_ONLY", "ALL_MEMBERS");
    private static final Set<String> NON_ADMIN_SCOPES = Set.of("INVITER_DEPT", "DESIGNATED_DEPTS");
    private static final Set<Integer> EXPIRE_DAYS = Set.of(1, 3, 7, 30);

    private final SysTenantInviteSettingsMapper inviteSettingsMapper;
    private final ObjectMapper objectMapper;

    @Override
    public InviteSettingsRespVO getSettings() {
        return getSettings(TenantContextHolder.getRequiredTenantId());
    }

    @Override
    public InviteSettingsRespVO getSettings(long tenantId) {
        SysTenantInviteSettingsDO row = findRow(tenantId);
        if (row == null) {
            return defaults();
        }
        return toResp(row);
    }

    @Override
    @Transactional
    public InviteSettingsRespVO updateSettings(InviteSettingsUpdateReqVO request) {
        validate(request);
        long tenantId = TenantContextHolder.getRequiredTenantId();
        SysTenantInviteSettingsDO row = findRow(tenantId);
        if (row == null) {
            row = new SysTenantInviteSettingsDO();
            row.setTenantId(tenantId);
            apply(row, request);
            inviteSettingsMapper.insert(row);
        } else {
            apply(row, request);
            inviteSettingsMapper.updateById(row);
        }
        return toResp(findRow(tenantId));
    }

    private void validate(InviteSettingsUpdateReqVO request) {
        String mode = normalize(request.getInviteMode());
        String scope = normalize(request.getNonAdminScope());
        if (!INVITE_MODES.contains(mode) || !NON_ADMIN_SCOPES.contains(scope)) {
            throw new ServiceException(ErrorCodeConstants.INVITE_SETTINGS_INVALID);
        }
        if (request.getNonAdminExpireDays() == null
                || !EXPIRE_DAYS.contains(request.getNonAdminExpireDays())) {
            throw new ServiceException(ErrorCodeConstants.INVITE_SETTINGS_INVALID);
        }
        if (request.getNonAdminDeptIds() == null || request.getNonAdminRequireApproval() == null) {
            throw new ServiceException(ErrorCodeConstants.INVITE_SETTINGS_INVALID);
        }
        for (String deptId : request.getNonAdminDeptIds()) {
            if (!StringUtils.hasText(deptId)) {
                throw new ServiceException(ErrorCodeConstants.INVITE_SETTINGS_INVALID);
            }
        }
        request.setInviteMode(mode);
        request.setNonAdminScope(scope);
    }

    private void apply(SysTenantInviteSettingsDO row, InviteSettingsUpdateReqVO request) {
        row.setInviteMode(request.getInviteMode());
        row.setNonAdminScope(request.getNonAdminScope());
        row.setNonAdminDeptIds(writeDeptIds(request.getNonAdminDeptIds()));
        row.setNonAdminExpireDays(request.getNonAdminExpireDays());
        row.setNonAdminRequireApproval(request.getNonAdminRequireApproval());
    }

    private SysTenantInviteSettingsDO findRow(long tenantId) {
        return inviteSettingsMapper.selectOne(new LambdaQueryWrapper<SysTenantInviteSettingsDO>()
                .eq(SysTenantInviteSettingsDO::getTenantId, tenantId)
                .last("LIMIT 1"));
    }

    private InviteSettingsRespVO defaults() {
        InviteSettingsRespVO resp = new InviteSettingsRespVO();
        resp.setInviteMode(DEFAULT_INVITE_MODE);
        resp.setNonAdminScope(DEFAULT_NON_ADMIN_SCOPE);
        resp.setNonAdminDeptIds(new ArrayList<>());
        resp.setNonAdminExpireDays(DEFAULT_EXPIRE_DAYS);
        resp.setNonAdminRequireApproval(false);
        return resp;
    }

    private InviteSettingsRespVO toResp(SysTenantInviteSettingsDO row) {
        if (row == null) {
            return defaults();
        }
        InviteSettingsRespVO resp = new InviteSettingsRespVO();
        resp.setInviteMode(row.getInviteMode() != null ? row.getInviteMode() : DEFAULT_INVITE_MODE);
        resp.setNonAdminScope(row.getNonAdminScope() != null ? row.getNonAdminScope() : DEFAULT_NON_ADMIN_SCOPE);
        resp.setNonAdminDeptIds(readDeptIds(row.getNonAdminDeptIds()));
        resp.setNonAdminExpireDays(row.getNonAdminExpireDays() != null
                ? row.getNonAdminExpireDays()
                : DEFAULT_EXPIRE_DAYS);
        resp.setNonAdminRequireApproval(Boolean.TRUE.equals(row.getNonAdminRequireApproval()));
        return resp;
    }

    private List<String> readDeptIds(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<>() {
            });
            return list != null ? new ArrayList<>(list) : new ArrayList<>();
        } catch (Exception ex) {
            throw new IllegalStateException("parse non_admin_dept_ids failed", ex);
        }
    }

    private String writeDeptIds(List<String> deptIds) {
        try {
            return objectMapper.writeValueAsString(deptIds != null ? deptIds : List.of());
        } catch (Exception ex) {
            throw new IllegalStateException("serialize non_admin_dept_ids failed", ex);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
