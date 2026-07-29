package com.relayflow.module.calendar.service.calendar;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.calendar.controller.app.vo.CalCalendarCreateReqVO;
import com.relayflow.module.calendar.controller.app.vo.CalCalendarRespVO;
import com.relayflow.module.calendar.controller.app.vo.CalCalendarUpdateReqVO;
import com.relayflow.module.calendar.convert.CalCalendarConvert;
import com.relayflow.module.calendar.dal.dataobject.CalCalendarDO;
import com.relayflow.module.calendar.dal.dataobject.CalEventDO;
import com.relayflow.module.calendar.dal.mapper.CalCalendarMapper;
import com.relayflow.module.calendar.dal.mapper.CalEventMapper;
import com.relayflow.module.calendar.enums.CalendarEventStatus;
import com.relayflow.module.calendar.enums.CalendarType;
import com.relayflow.module.calendar.enums.ErrorCodeConstants;
import com.relayflow.module.calendar.service.allstaff.CalAllStaffService;
import com.relayflow.module.calendar.service.pref.CalCalendarUserPrefService;
import com.relayflow.module.calendar.service.settings.CalTenantSettingsService;
import com.relayflow.module.calendar.service.share.CalCalendarShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalCalendarServiceImpl implements CalCalendarService {

    private static final String DEFAULT_PRIMARY_NAME = "我的日历";
    private static final String DEFAULT_PRIMARY_COLOR = "#3B82F6";
    private static final String ALL_STAFF_WRITE_AUTHORITY = "calendar:product-settings:update";

    private final CalCalendarMapper calCalendarMapper;
    private final CalEventMapper calEventMapper;
    /** Avoid construct cycle with CalCalendarShareService → CalCalendarService. */
    private final ObjectProvider<CalCalendarShareService> calCalendarShareService;
    private final ObjectProvider<CalCalendarUserPrefService> calCalendarUserPrefService;
    private final ObjectProvider<CalAllStaffService> calAllStaffService;
    private final ObjectProvider<CalTenantSettingsService> calTenantSettingsService;

    @Override
    public List<CalCalendarRespVO> listMine() {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        ensurePrimary(tenantId, userId);
        List<CalCalendarRespVO> owned = calCalendarMapper.selectList(
                        Wrappers.<CalCalendarDO>lambdaQuery()
                                .eq(CalCalendarDO::getOwnerUserId, userId)
                                .in(CalCalendarDO::getType, CalendarType.PRIMARY.name(), CalendarType.OWNED.name())
                                .orderByAsc(CalCalendarDO::getType)
                                .orderByAsc(CalCalendarDO::getCreateTime))
                .stream()
                .map(CalCalendarConvert.INSTANCE::toResp)
                .toList();
        applyDisplayColors(userId, owned);
        return owned;
    }

    @Override
    public List<CalCalendarRespVO> listVisible() {
        List<CalCalendarRespVO> owned = listMine();
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        CalCalendarShareService shareService = calCalendarShareService.getObject();
        Map<Long, String> permissions = shareService.sharedCalendarPermissions(userId);
        List<CalCalendarDO> sharedCalendars = shareService.loadSharedCalendars(userId);

        Set<Long> seen = owned.stream()
                .map(CalCalendarRespVO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<CalCalendarRespVO> result = new ArrayList<>(owned);
        for (CalCalendarDO calendar : sharedCalendars) {
            if (calendar != null && seen.add(calendar.getId())) {
                result.add(CalCalendarConvert.INSTANCE.toSharedResp(calendar, permissions.get(calendar.getId())));
            }
        }
        for (CalCalendarDO calendar : calAllStaffService.getObject().loadVisibleCalendars(userId)) {
            if (calendar != null && seen.add(calendar.getId())) {
                result.add(CalCalendarConvert.INSTANCE.toAllStaffResp(calendar));
            }
        }
        applyDisplayColors(userId, result);
        return result;
    }

    private void applyDisplayColors(Long userId, List<CalCalendarRespVO> calendars) {
        if (calendars.isEmpty()) {
            return;
        }
        Set<Long> ids = calendars.stream()
                .map(CalCalendarRespVO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, String> displayColors = calCalendarUserPrefService.getObject().displayColors(userId, ids);
        for (CalCalendarRespVO vo : calendars) {
            String defaultColor = vo.getColor() != null ? vo.getColor() : DEFAULT_PRIMARY_COLOR;
            vo.setDefaultColor(defaultColor);
            String display = displayColors.get(vo.getId());
            vo.setDisplayColor(display);
            vo.setEffectiveColor(StringUtils.hasText(display) ? display : defaultColor);
            // Keep color as default for backward-compatible clients; UI prefers effectiveColor.
            vo.setColor(defaultColor);
        }
    }

    @Override
    public Long create(CalCalendarCreateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        if (!calTenantSettingsService.getObject().allowMemberCreateOwnedCalendar(tenantId)) {
            throw new ServiceException(ErrorCodeConstants.OWNED_CALENDAR_CREATE_FORBIDDEN);
        }
        ensurePrimary(tenantId, userId);

        OffsetDateTime now = OffsetDateTime.now();
        CalCalendarDO row = new CalCalendarDO();
        row.setTenantId(tenantId);
        row.setOwnerUserId(userId);
        row.setName(request.getName().trim());
        row.setColor(request.getColor().trim());
        row.setDescription(blankToNull(request.getDescription()));
        row.setType(CalendarType.OWNED.name());
        row.setCreator(userId);
        row.setCreateTime(now);
        row.setUpdater(userId);
        row.setUpdateTime(now);
        calCalendarMapper.insert(row);
        return row.getId();
    }

    @Override
    public void update(CalCalendarUpdateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        CalCalendarDO row = calCalendarMapper.selectById(request.getId());
        if (row == null) {
            throw new ServiceException(ErrorCodeConstants.CALENDAR_NOT_FOUND);
        }
        if (CalendarType.ALL_STAFF.name().equals(row.getType())) {
            if (!canManageAllStaffCalendars()) {
                throw new ServiceException(ErrorCodeConstants.CALENDAR_FORBIDDEN);
            }
            // Name/description only; default color stays admin product-settings only.
            row.setName(request.getName().trim());
            row.setDescription(blankToNull(request.getDescription()));
            row.setUpdater(userId);
            row.setUpdateTime(OffsetDateTime.now());
            calCalendarMapper.updateById(row);
            return;
        }
        if (!Objects.equals(row.getOwnerUserId(), userId)) {
            throw new ServiceException(ErrorCodeConstants.CALENDAR_FORBIDDEN);
        }
        row.setName(request.getName().trim());
        row.setColor(request.getColor().trim());
        row.setDescription(blankToNull(request.getDescription()));
        row.setUpdater(userId);
        row.setUpdateTime(OffsetDateTime.now());
        calCalendarMapper.updateById(row);
    }

    @Override
    public void delete(Long id) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        CalCalendarDO row = requireOwnedCalendar(id, userId);
        if (CalendarType.PRIMARY.name().equals(row.getType())) {
            throw new ServiceException(ErrorCodeConstants.PRIMARY_CALENDAR_DELETE_FORBIDDEN);
        }
        if (CalendarType.ALL_STAFF.name().equals(row.getType())) {
            throw new ServiceException(ErrorCodeConstants.CALENDAR_FORBIDDEN);
        }
        Long activeEvents = calEventMapper.selectCount(
                Wrappers.<CalEventDO>lambdaQuery()
                        .eq(CalEventDO::getCalendarId, id)
                        .ne(CalEventDO::getStatus, CalendarEventStatus.CANCELLED.name()));
        if (activeEvents != null && activeEvents > 0) {
            throw new ServiceException(ErrorCodeConstants.CALENDAR_NOT_EMPTY);
        }
        calCalendarMapper.deleteById(id);
    }

    @Override
    public void updateDisplayColor(Long calendarId, String displayColor) {
        calCalendarUserPrefService.getObject().updateDisplayColor(calendarId, displayColor);
    }

    @Override
    public CalCalendarDO ensurePrimary(Long tenantId, Long userId) {
        CalCalendarDO existing = calCalendarMapper.selectOne(
                Wrappers.<CalCalendarDO>lambdaQuery()
                        .eq(CalCalendarDO::getOwnerUserId, userId)
                        .eq(CalCalendarDO::getType, CalendarType.PRIMARY.name())
                        .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        OffsetDateTime now = OffsetDateTime.now();
        CalCalendarDO row = new CalCalendarDO();
        row.setTenantId(tenantId);
        row.setOwnerUserId(userId);
        row.setName(DEFAULT_PRIMARY_NAME);
        row.setColor(DEFAULT_PRIMARY_COLOR);
        row.setDescription(null);
        row.setType(CalendarType.PRIMARY.name());
        row.setCreator(userId);
        row.setCreateTime(now);
        row.setUpdater(userId);
        row.setUpdateTime(now);
        try {
            calCalendarMapper.insert(row);
            return row;
        } catch (DuplicateKeyException ex) {
            CalCalendarDO raced = calCalendarMapper.selectOne(
                    Wrappers.<CalCalendarDO>lambdaQuery()
                            .eq(CalCalendarDO::getOwnerUserId, userId)
                            .eq(CalCalendarDO::getType, CalendarType.PRIMARY.name())
                            .last("LIMIT 1"));
            if (raced != null) {
                return raced;
            }
            throw ex;
        }
    }

    @Override
    public CalCalendarDO requireOwnedCalendar(Long calendarId, Long userId) {
        CalCalendarDO row = calCalendarMapper.selectById(calendarId);
        if (row == null) {
            throw new ServiceException(ErrorCodeConstants.CALENDAR_NOT_FOUND);
        }
        if (!Objects.equals(row.getOwnerUserId(), userId)) {
            throw new ServiceException(ErrorCodeConstants.CALENDAR_FORBIDDEN);
        }
        return row;
    }

    @Override
    public CalCalendarDO requireWritableCalendar(Long calendarId, Long userId) {
        CalCalendarDO row = calCalendarMapper.selectById(calendarId);
        if (row == null) {
            throw new ServiceException(ErrorCodeConstants.CALENDAR_NOT_FOUND);
        }
        String type = row.getType();
        if (CalendarType.PRIMARY.name().equals(type) || CalendarType.OWNED.name().equals(type)) {
            if (!Objects.equals(row.getOwnerUserId(), userId)) {
                throw new ServiceException(ErrorCodeConstants.CALENDAR_FORBIDDEN);
            }
            return row;
        }
        if (CalendarType.ALL_STAFF.name().equals(type)) {
            if (!canManageAllStaffCalendars()) {
                throw new ServiceException(ErrorCodeConstants.CALENDAR_FORBIDDEN);
            }
            return row;
        }
        throw new ServiceException(ErrorCodeConstants.CALENDAR_FORBIDDEN);
    }

    @Override
    public boolean canManageAllStaffCalendars() {
        return SecurityFrameworkUtils.hasAuthority(ALL_STAFF_WRITE_AUTHORITY);
    }

    private static String blankToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
