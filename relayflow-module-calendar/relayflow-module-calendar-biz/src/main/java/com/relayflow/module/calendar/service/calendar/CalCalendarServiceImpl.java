package com.relayflow.module.calendar.service.calendar;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.calendar.controller.app.vo.CalCalendarCreateReqVO;
import com.relayflow.module.calendar.controller.app.vo.CalCalendarRespVO;
import com.relayflow.module.calendar.controller.app.vo.CalCalendarUpdateReqVO;
import com.relayflow.module.calendar.dal.dataobject.CalCalendarDO;
import com.relayflow.module.calendar.dal.dataobject.CalEventDO;
import com.relayflow.module.calendar.dal.mapper.CalCalendarMapper;
import com.relayflow.module.calendar.dal.mapper.CalEventMapper;
import com.relayflow.module.calendar.enums.CalendarEventStatus;
import com.relayflow.module.calendar.enums.CalendarType;
import com.relayflow.module.calendar.enums.ErrorCodeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CalCalendarServiceImpl implements CalCalendarService {

    private static final String DEFAULT_PRIMARY_NAME = "我的日历";
    private static final String DEFAULT_PRIMARY_COLOR = "#3B82F6";

    private final CalCalendarMapper calCalendarMapper;
    private final CalEventMapper calEventMapper;

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
                .map(this::toResp)
                .toList();
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

    private CalCalendarRespVO toResp(CalCalendarDO row) {
        CalCalendarRespVO vo = new CalCalendarRespVO();
        vo.setId(row.getId());
        vo.setName(row.getName());
        vo.setColor(row.getColor());
        vo.setDescription(row.getDescription());
        vo.setType(row.getType());
        return vo;
    }

    private static String blankToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
