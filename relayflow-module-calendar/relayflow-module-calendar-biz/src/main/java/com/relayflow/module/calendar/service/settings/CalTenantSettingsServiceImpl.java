package com.relayflow.module.calendar.service.settings;

import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.calendar.controller.admin.vo.CalProductSettingsRespVO;
import com.relayflow.module.calendar.controller.admin.vo.CalProductSettingsUpdateReqVO;
import com.relayflow.module.calendar.dal.dataobject.CalTenantSettingsDO;
import com.relayflow.module.calendar.dal.mapper.CalTenantSettingsMapper;
import com.relayflow.module.calendar.service.allstaff.CalAllStaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class CalTenantSettingsServiceImpl implements CalTenantSettingsService {

    private final CalTenantSettingsMapper settingsMapper;
    private final CalAllStaffService calAllStaffService;

    @Override
    public CalProductSettingsRespVO getSettings() {
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        CalProductSettingsRespVO vo = new CalProductSettingsRespVO();
        vo.setAllowMemberCreateOwnedCalendar(allowMemberCreateOwnedCalendar(tenantId));
        vo.setAllStaffCalendars(calAllStaffService.listAdmin());
        return vo;
    }

    @Override
    @Transactional
    public CalProductSettingsRespVO updateSettings(CalProductSettingsUpdateReqVO request) {
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        OffsetDateTime now = OffsetDateTime.now();
        CalTenantSettingsDO row = settingsMapper.selectById(tenantId);
        int allow = Boolean.TRUE.equals(request.getAllowMemberCreateOwnedCalendar()) ? 1 : 0;
        if (row == null) {
            row = new CalTenantSettingsDO();
            row.setTenantId(tenantId);
            row.setAllowMemberCreateOwnedCalendar(allow);
            row.setCreator(userId);
            row.setCreateTime(now);
            row.setUpdater(userId);
            row.setUpdateTime(now);
            settingsMapper.insert(row);
        } else {
            row.setAllowMemberCreateOwnedCalendar(allow);
            row.setUpdater(userId);
            row.setUpdateTime(now);
            settingsMapper.updateById(row);
        }
        return getSettings();
    }

    @Override
    public boolean allowMemberCreateOwnedCalendar(Long tenantId) {
        CalTenantSettingsDO row = settingsMapper.selectById(tenantId);
        if (row == null || row.getAllowMemberCreateOwnedCalendar() == null) {
            return true;
        }
        return row.getAllowMemberCreateOwnedCalendar() != 0;
    }
}
