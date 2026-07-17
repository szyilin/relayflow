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

    private final CalCalendarMapper calCalendarMapper;
    private final CalEventMapper calEventMapper;
    /** Avoid construct cycle with CalCalendarShareService → CalCalendarService. */
    private final ObjectProvider<CalCalendarShareService> calCalendarShareService;

    @Override
    public List<CalCalendarRespVO> listMine() {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        ensurePrimary(tenantId, userId);
        return calCalendarMapper.selectList(
                        Wrappers.<CalCalendarDO>lambdaQuery()
                                .eq(CalCalendarDO::getOwnerUserId, userId)
                                .orderByAsc(CalCalendarDO::getType)
                                .orderByAsc(CalCalendarDO::getCreateTime))
                .stream()
                .map(CalCalendarConvert.INSTANCE::toResp)
                .toList();
    }

    @Override
    public List<CalCalendarRespVO> listVisible() {
        List<CalCalendarRespVO> owned = listMine();
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        CalCalendarShareService shareService = calCalendarShareService.getObject();
        Map<Long, String> permissions = shareService.sharedCalendarPermissions(userId);
        List<CalCalendarDO> sharedCalendars = shareService.loadSharedCalendars(userId);

        Set<Long> ownedIds = owned.stream()
                .map(CalCalendarRespVO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<CalCalendarRespVO> result = new ArrayList<>(owned);
        for (CalCalendarDO calendar : sharedCalendars) {
            if (!ownedIds.contains(calendar.getId())) {
                result.add(CalCalendarConvert.INSTANCE.toSharedResp(calendar, permissions.get(calendar.getId())));
            }
        }
        return result;
    }

    @Override
    public Long create(CalCalendarCreateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
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
        CalCalendarDO row = requireOwnedCalendar(request.getId(), userId);
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

    private static String blankToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
