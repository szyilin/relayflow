package com.relayflow.module.calendar.service.allstaff;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.calendar.controller.admin.vo.CalAllStaffCalendarRespVO;
import com.relayflow.module.calendar.controller.admin.vo.CalAllStaffSaveReqVO;
import com.relayflow.module.calendar.controller.admin.vo.CalAllStaffScopeItemVO;
import com.relayflow.module.calendar.dal.dataobject.CalAllStaffScopeDO;
import com.relayflow.module.calendar.dal.dataobject.CalCalendarDO;
import com.relayflow.module.calendar.dal.dataobject.CalEventDO;
import com.relayflow.module.calendar.dal.mapper.CalAllStaffScopeMapper;
import com.relayflow.module.calendar.dal.mapper.CalCalendarMapper;
import com.relayflow.module.calendar.dal.mapper.CalEventMapper;
import com.relayflow.module.calendar.enums.CalendarEventStatus;
import com.relayflow.module.calendar.enums.CalendarType;
import com.relayflow.module.calendar.enums.ErrorCodeConstants;
import com.relayflow.module.system.api.dept.DeptApi;
import com.relayflow.module.system.api.tenant.TenantMemberApi;
import com.relayflow.module.system.api.user.UserApi;
import com.relayflow.module.system.api.user.dto.UserBasicDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CalAllStaffServiceImpl implements CalAllStaffService {

    private static final String SCOPE_ALL = "ALL";
    private static final String SCOPE_USER = "USER";
    private static final String SCOPE_DEPT = "DEPT";

    private final CalCalendarMapper calCalendarMapper;
    private final CalAllStaffScopeMapper scopeMapper;
    private final CalEventMapper calEventMapper;
    private final UserApi userApi;
    private final DeptApi deptApi;
    private final TenantMemberApi tenantMemberApi;

    @Override
    public List<CalAllStaffCalendarRespVO> listAdmin() {
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        List<CalCalendarDO> calendars = calCalendarMapper.selectList(
                Wrappers.<CalCalendarDO>lambdaQuery()
                        .eq(CalCalendarDO::getType, CalendarType.ALL_STAFF.name())
                        .orderByDesc(CalCalendarDO::getCreateTime));
        if (calendars.isEmpty()) {
            return List.of();
        }
        Set<Long> ids = calendars.stream().map(CalCalendarDO::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, List<CalAllStaffScopeDO>> scopesByCal = loadScopes(tenantId, ids);
        List<CalAllStaffCalendarRespVO> result = new ArrayList<>(calendars.size());
        for (CalCalendarDO calendar : calendars) {
            result.add(toAdminResp(calendar, scopesByCal.getOrDefault(calendar.getId(), List.of())));
        }
        return result;
    }

    @Override
    @Transactional
    public Long create(CalAllStaffSaveReqVO request) {
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        validateScopes(tenantId, request.getScopes());

        OffsetDateTime now = OffsetDateTime.now();
        CalCalendarDO row = new CalCalendarDO();
        row.setTenantId(tenantId);
        row.setOwnerUserId(userId);
        row.setName(request.getName().trim());
        row.setColor(request.getColor().trim());
        row.setDescription(blankToNull(request.getDescription()));
        row.setType(CalendarType.ALL_STAFF.name());
        row.setCreator(userId);
        row.setCreateTime(now);
        row.setUpdater(userId);
        row.setUpdateTime(now);
        calCalendarMapper.insert(row);
        replaceScopes(tenantId, userId, row.getId(), request.getScopes(), now);
        return row.getId();
    }

    @Override
    @Transactional
    public void update(CalAllStaffSaveReqVO request) {
        if (request.getId() == null) {
            throw new ServiceException(ErrorCodeConstants.ALL_STAFF_NOT_FOUND);
        }
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        CalCalendarDO row = requireAllStaff(request.getId());
        validateScopes(tenantId, request.getScopes());

        OffsetDateTime now = OffsetDateTime.now();
        row.setName(request.getName().trim());
        row.setColor(request.getColor().trim());
        row.setDescription(blankToNull(request.getDescription()));
        row.setUpdater(userId);
        row.setUpdateTime(now);
        calCalendarMapper.updateById(row);
        replaceScopes(tenantId, userId, row.getId(), request.getScopes(), now);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CalCalendarDO row = requireAllStaff(id);
        Long count = calEventMapper.selectCount(
                Wrappers.<CalEventDO>lambdaQuery()
                        .eq(CalEventDO::getCalendarId, id)
                        .ne(CalEventDO::getStatus, CalendarEventStatus.CANCELLED.name()));
        if (count != null && count > 0) {
            throw new ServiceException(ErrorCodeConstants.CALENDAR_NOT_EMPTY);
        }
        scopeMapper.delete(Wrappers.<CalAllStaffScopeDO>lambdaQuery()
                .eq(CalAllStaffScopeDO::getCalendarId, id));
        calCalendarMapper.deleteById(row.getId());
    }

    @Override
    public Set<Long> visibleCalendarIds(Long userId) {
        return loadVisibleCalendars(userId).stream()
                .map(CalCalendarDO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public List<CalCalendarDO> loadVisibleCalendars(Long userId) {
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        List<CalCalendarDO> allStaff = calCalendarMapper.selectList(
                Wrappers.<CalCalendarDO>lambdaQuery()
                        .eq(CalCalendarDO::getType, CalendarType.ALL_STAFF.name()));
        if (allStaff.isEmpty()) {
            return List.of();
        }
        Set<Long> ids = allStaff.stream().map(CalCalendarDO::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, List<CalAllStaffScopeDO>> scopesByCal = loadScopes(tenantId, ids);
        List<Long> userDeptIds = safeList(userApi.listUserDeptIds(userId));
        Set<Long> userDeptSet = new HashSet<>(userDeptIds);

        List<CalCalendarDO> visible = new ArrayList<>();
        for (CalCalendarDO calendar : allStaff) {
            if (matchesScope(scopesByCal.getOrDefault(calendar.getId(), List.of()), userId, userDeptSet)) {
                visible.add(calendar);
            }
        }
        return visible;
    }

    @Override
    public boolean isVisibleToUser(Long calendarId, Long userId) {
        return visibleCalendarIds(userId).contains(calendarId);
    }

    private CalCalendarDO requireAllStaff(Long id) {
        CalCalendarDO row = calCalendarMapper.selectById(id);
        if (row == null || !CalendarType.ALL_STAFF.name().equals(row.getType())) {
            throw new ServiceException(ErrorCodeConstants.ALL_STAFF_NOT_FOUND);
        }
        return row;
    }

    private void validateScopes(Long tenantId, List<CalAllStaffScopeItemVO> scopes) {
        if (CollectionUtils.isEmpty(scopes)) {
            throw new ServiceException(ErrorCodeConstants.ALL_STAFF_SCOPE_INVALID);
        }
        boolean hasAll = false;
        Set<String> dedupe = new HashSet<>();
        for (CalAllStaffScopeItemVO item : scopes) {
            if (item == null || !StringUtils.hasText(item.getScopeType())) {
                throw new ServiceException(ErrorCodeConstants.ALL_STAFF_SCOPE_INVALID);
            }
            String type = item.getScopeType().trim().toUpperCase();
            item.setScopeType(type);
            if (SCOPE_ALL.equals(type)) {
                hasAll = true;
                item.setScopeId(null);
                continue;
            }
            if (!SCOPE_USER.equals(type) && !SCOPE_DEPT.equals(type)) {
                throw new ServiceException(ErrorCodeConstants.ALL_STAFF_SCOPE_INVALID);
            }
            if (item.getScopeId() == null) {
                throw new ServiceException(ErrorCodeConstants.ALL_STAFF_SCOPE_INVALID);
            }
            String key = type + ":" + item.getScopeId();
            if (!dedupe.add(key)) {
                continue;
            }
            if (SCOPE_USER.equals(type)) {
                Set<Long> active = tenantMemberApi.filterActiveMemberUserIds(tenantId, List.of(item.getScopeId()));
                if (active == null || !active.contains(item.getScopeId())) {
                    throw new ServiceException(ErrorCodeConstants.ALL_STAFF_SCOPE_INVALID);
                }
            } else if (!deptApi.existsInTenant(tenantId, item.getScopeId())) {
                throw new ServiceException(ErrorCodeConstants.ALL_STAFF_SCOPE_INVALID);
            }
        }
        if (hasAll && scopes.size() > 1) {
            // Normalize: ALL alone is enough
            scopes.clear();
            CalAllStaffScopeItemVO all = new CalAllStaffScopeItemVO();
            all.setScopeType(SCOPE_ALL);
            scopes.add(all);
        }
    }

    private void replaceScopes(Long tenantId, Long userId, Long calendarId,
                               List<CalAllStaffScopeItemVO> scopes, OffsetDateTime now) {
        scopeMapper.delete(Wrappers.<CalAllStaffScopeDO>lambdaQuery()
                .eq(CalAllStaffScopeDO::getCalendarId, calendarId));
        for (CalAllStaffScopeItemVO item : scopes) {
            CalAllStaffScopeDO row = new CalAllStaffScopeDO();
            row.setTenantId(tenantId);
            row.setCalendarId(calendarId);
            row.setScopeType(item.getScopeType());
            row.setScopeId(item.getScopeId());
            row.setCreator(userId);
            row.setCreateTime(now);
            row.setUpdater(userId);
            row.setUpdateTime(now);
            scopeMapper.insert(row);
        }
    }

    private Map<Long, List<CalAllStaffScopeDO>> loadScopes(Long tenantId, Set<Long> calendarIds) {
        if (calendarIds.isEmpty()) {
            return Map.of();
        }
        List<CalAllStaffScopeDO> rows = scopeMapper.selectList(
                Wrappers.<CalAllStaffScopeDO>lambdaQuery()
                        .eq(CalAllStaffScopeDO::getTenantId, tenantId)
                        .in(CalAllStaffScopeDO::getCalendarId, calendarIds));
        Map<Long, List<CalAllStaffScopeDO>> map = new LinkedHashMap<>();
        for (CalAllStaffScopeDO row : rows) {
            map.computeIfAbsent(row.getCalendarId(), k -> new ArrayList<>()).add(row);
        }
        return map;
    }

    private boolean matchesScope(List<CalAllStaffScopeDO> scopes, Long userId, Set<Long> userDeptIds) {
        if (scopes.isEmpty()) {
            return false;
        }
        for (CalAllStaffScopeDO scope : scopes) {
            if (SCOPE_ALL.equals(scope.getScopeType())) {
                return true;
            }
            if (SCOPE_USER.equals(scope.getScopeType()) && Objects.equals(scope.getScopeId(), userId)) {
                return true;
            }
            if (SCOPE_DEPT.equals(scope.getScopeType())
                    && scope.getScopeId() != null
                    && userDeptIds.contains(scope.getScopeId())) {
                return true;
            }
        }
        return false;
    }

    private CalAllStaffCalendarRespVO toAdminResp(CalCalendarDO calendar, List<CalAllStaffScopeDO> scopes) {
        CalAllStaffCalendarRespVO vo = new CalAllStaffCalendarRespVO();
        vo.setId(calendar.getId());
        vo.setName(calendar.getName());
        vo.setColor(calendar.getColor());
        vo.setDescription(calendar.getDescription());
        vo.setOwnerUserId(calendar.getOwnerUserId());
        List<CalAllStaffScopeItemVO> items = new ArrayList<>(scopes.size());
        for (CalAllStaffScopeDO scope : scopes) {
            CalAllStaffScopeItemVO item = new CalAllStaffScopeItemVO();
            item.setScopeType(scope.getScopeType());
            item.setScopeId(scope.getScopeId());
            item.setScopeLabel(resolveScopeLabel(scope));
            items.add(item);
        }
        vo.setScopes(items);
        return vo;
    }

    private String resolveScopeLabel(CalAllStaffScopeDO scope) {
        if (SCOPE_ALL.equals(scope.getScopeType())) {
            return "全员";
        }
        if (scope.getScopeId() == null) {
            return scope.getScopeType();
        }
        try {
            if (SCOPE_USER.equals(scope.getScopeType())) {
                UserBasicDTO basic = userApi.getUserBasic(scope.getScopeId());
                if (basic != null && StringUtils.hasText(basic.getNickname())) {
                    return basic.getNickname();
                }
                if (basic != null && StringUtils.hasText(basic.getUsername())) {
                    return basic.getUsername();
                }
            } else if (SCOPE_DEPT.equals(scope.getScopeType())) {
                String name = deptApi.getDeptName(SecurityFrameworkUtils.requireLoginTenantId(), scope.getScopeId());
                if (StringUtils.hasText(name)) {
                    return name;
                }
            }
        } catch (Exception ignored) {
            // best-effort labels
        }
        return scope.getScopeType() + ":" + scope.getScopeId();
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static List<Long> safeList(List<Long> list) {
        return list == null ? List.of() : list;
    }
}
