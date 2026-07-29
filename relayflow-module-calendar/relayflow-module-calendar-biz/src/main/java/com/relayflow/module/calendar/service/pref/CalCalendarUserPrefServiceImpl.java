package com.relayflow.module.calendar.service.pref;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.calendar.dal.dataobject.CalCalendarDO;
import com.relayflow.module.calendar.dal.dataobject.CalCalendarUserPrefDO;
import com.relayflow.module.calendar.dal.mapper.CalCalendarMapper;
import com.relayflow.module.calendar.dal.mapper.CalCalendarUserPrefMapper;
import com.relayflow.module.calendar.enums.ErrorCodeConstants;
import com.relayflow.module.calendar.service.allstaff.CalAllStaffService;
import com.relayflow.module.calendar.service.share.CalCalendarShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CalCalendarUserPrefServiceImpl implements CalCalendarUserPrefService {

    private final CalCalendarUserPrefMapper userPrefMapper;
    private final CalCalendarMapper calCalendarMapper;
    private final ObjectProvider<CalCalendarShareService> calCalendarShareService;
    private final ObjectProvider<CalAllStaffService> calAllStaffService;

    @Override
    public Map<Long, String> displayColors(Long userId, Collection<Long> calendarIds) {
        if (calendarIds == null || calendarIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<CalCalendarUserPrefDO> rows = userPrefMapper.selectList(
                Wrappers.<CalCalendarUserPrefDO>lambdaQuery()
                        .eq(CalCalendarUserPrefDO::getUserId, userId)
                        .in(CalCalendarUserPrefDO::getCalendarId, calendarIds));
        Map<Long, String> result = new HashMap<>();
        for (CalCalendarUserPrefDO row : rows) {
            if (StringUtils.hasText(row.getDisplayColor())) {
                result.put(row.getCalendarId(), row.getDisplayColor().trim());
            }
        }
        return result;
    }

    @Override
    public void updateDisplayColor(Long calendarId, String displayColor) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        requireVisibleCalendar(calendarId, userId);

        String normalized = blankToNull(displayColor);
        OffsetDateTime now = OffsetDateTime.now();
        CalCalendarUserPrefDO existing = userPrefMapper.selectOne(
                Wrappers.<CalCalendarUserPrefDO>lambdaQuery()
                        .eq(CalCalendarUserPrefDO::getUserId, userId)
                        .eq(CalCalendarUserPrefDO::getCalendarId, calendarId)
                        .last("LIMIT 1"));
        if (existing == null) {
            if (normalized == null) {
                return;
            }
            CalCalendarUserPrefDO row = new CalCalendarUserPrefDO();
            row.setTenantId(tenantId);
            row.setUserId(userId);
            row.setCalendarId(calendarId);
            row.setDisplayColor(normalized);
            row.setCreator(userId);
            row.setCreateTime(now);
            row.setUpdater(userId);
            row.setUpdateTime(now);
            userPrefMapper.insert(row);
            return;
        }
        existing.setDisplayColor(normalized);
        existing.setUpdater(userId);
        existing.setUpdateTime(now);
        userPrefMapper.updateById(existing);
    }

    private void requireVisibleCalendar(Long calendarId, Long userId) {
        CalCalendarDO calendar = calCalendarMapper.selectById(calendarId);
        if (calendar == null) {
            throw new ServiceException(ErrorCodeConstants.CALENDAR_NOT_FOUND);
        }
        if (Objects.equals(calendar.getOwnerUserId(), userId)) {
            return;
        }
        Map<Long, String> shared = calCalendarShareService.getObject().sharedCalendarPermissions(userId);
        if (shared.containsKey(calendarId)) {
            return;
        }
        if (calAllStaffService.getObject().isVisibleToUser(calendarId, userId)) {
            return;
        }
        throw new ServiceException(ErrorCodeConstants.CALENDAR_FORBIDDEN);
    }

    private static String blankToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
