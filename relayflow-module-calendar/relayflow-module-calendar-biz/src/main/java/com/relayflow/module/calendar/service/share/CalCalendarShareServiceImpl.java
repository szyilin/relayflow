package com.relayflow.module.calendar.service.share;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.calendar.controller.app.vo.CalCalendarShareCreateReqVO;
import com.relayflow.module.calendar.controller.app.vo.CalCalendarShareRespVO;
import com.relayflow.module.calendar.dal.dataobject.CalCalendarDO;
import com.relayflow.module.calendar.dal.dataobject.CalCalendarShareDO;
import com.relayflow.module.calendar.dal.mapper.CalCalendarMapper;
import com.relayflow.module.calendar.dal.mapper.CalCalendarShareMapper;
import com.relayflow.module.calendar.enums.ErrorCodeConstants;
import com.relayflow.module.calendar.service.calendar.CalCalendarService;
import com.relayflow.module.calendar.service.notify.CalendarBotNotifyService;
import com.relayflow.module.system.api.tenant.TenantMemberApi;
import com.relayflow.module.system.api.user.UserApi;
import com.relayflow.module.system.api.user.dto.UserBasicDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalCalendarShareServiceImpl implements CalCalendarShareService {

    private static final String PERMISSION_READ = "READ";

    private final CalCalendarShareMapper shareMapper;
    private final CalCalendarMapper calendarMapper;
    private final CalCalendarService calendarService;
    private final TenantMemberApi tenantMemberApi;
    private final UserApi userApi;
    private final CalendarBotNotifyService calendarBotNotifyService;

    @Override
    public List<CalCalendarShareRespVO> listMine() {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        calendarService.ensurePrimary(tenantId, userId);

        Set<Long> ownedIds = calendarMapper.selectList(
                        Wrappers.<CalCalendarDO>lambdaQuery().eq(CalCalendarDO::getOwnerUserId, userId))
                .stream()
                .map(CalCalendarDO::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<CalCalendarShareDO> outgoing = ownedIds.isEmpty()
                ? List.of()
                : shareMapper.selectList(
                Wrappers.<CalCalendarShareDO>lambdaQuery().in(CalCalendarShareDO::getCalendarId, ownedIds));
        List<CalCalendarShareDO> incoming = shareMapper.selectList(
                Wrappers.<CalCalendarShareDO>lambdaQuery().eq(CalCalendarShareDO::getGranteeUserId, userId));

        Set<Long> calendarIds = new LinkedHashSet<>();
        for (CalCalendarShareDO row : outgoing) {
            calendarIds.add(row.getCalendarId());
        }
        for (CalCalendarShareDO row : incoming) {
            calendarIds.add(row.getCalendarId());
        }
        Map<Long, CalCalendarDO> calendars = calendarIds.isEmpty()
                ? Map.of()
                : calendarMapper.selectBatchIds(calendarIds).stream()
                .collect(Collectors.toMap(CalCalendarDO::getId, c -> c, (a, b) -> a, HashMap::new));

        List<CalCalendarShareRespVO> result = new ArrayList<>();
        for (CalCalendarShareDO row : outgoing) {
            result.add(toResp(row, calendars.get(row.getCalendarId()), "OUTGOING"));
        }
        for (CalCalendarShareDO row : incoming) {
            result.add(toResp(row, calendars.get(row.getCalendarId()), "INCOMING"));
        }
        return result;
    }

    @Override
    @Transactional
    public Long create(CalCalendarShareCreateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        CalCalendarDO calendar = calendarService.requireOwnedCalendar(request.getCalendarId(), userId);
        Long grantee = request.getGranteeUserId();
        if (Objects.equals(grantee, userId)) {
            throw new ServiceException(ErrorCodeConstants.SHARE_SELF_FORBIDDEN);
        }
        Set<Long> active = tenantMemberApi.filterActiveMemberUserIds(tenantId, Set.of(grantee));
        if (!active.contains(grantee)) {
            throw new ServiceException(ErrorCodeConstants.SHARE_GRANTEE_INVALID);
        }
        String permission = StringUtils.hasText(request.getPermission())
                ? request.getPermission().trim().toUpperCase()
                : PERMISSION_READ;
        if (!PERMISSION_READ.equals(permission)) {
            throw new ServiceException(ErrorCodeConstants.SHARE_GRANTEE_INVALID);
        }

        CalCalendarShareDO existing = shareMapper.selectOne(
                Wrappers.<CalCalendarShareDO>lambdaQuery()
                        .eq(CalCalendarShareDO::getCalendarId, calendar.getId())
                        .eq(CalCalendarShareDO::getGranteeUserId, grantee)
                        .last("LIMIT 1"));
        if (existing != null) {
            return existing.getId();
        }

        OffsetDateTime now = OffsetDateTime.now();
        CalCalendarShareDO row = new CalCalendarShareDO();
        row.setTenantId(tenantId);
        row.setCalendarId(calendar.getId());
        row.setGranteeUserId(grantee);
        row.setPermission(permission);
        row.setCreator(userId);
        row.setCreateTime(now);
        row.setUpdater(userId);
        row.setUpdateTime(now);
        shareMapper.insert(row);
        calendarBotNotifyService.notifyCalendarShared(calendar, grantee);
        return row.getId();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        CalCalendarShareDO row = shareMapper.selectById(id);
        if (row == null) {
            throw new ServiceException(ErrorCodeConstants.SHARE_NOT_FOUND);
        }
        CalCalendarDO calendar = calendarMapper.selectById(row.getCalendarId());
        boolean owner = calendar != null && Objects.equals(calendar.getOwnerUserId(), userId);
        boolean grantee = Objects.equals(row.getGranteeUserId(), userId);
        if (!owner && !grantee) {
            throw new ServiceException(ErrorCodeConstants.SHARE_FORBIDDEN);
        }
        shareMapper.deleteById(id);
    }

    @Override
    public Map<Long, String> sharedCalendarPermissions(Long userId) {
        List<CalCalendarShareDO> rows = shareMapper.selectList(
                Wrappers.<CalCalendarShareDO>lambdaQuery().eq(CalCalendarShareDO::getGranteeUserId, userId));
        Map<Long, String> map = new HashMap<>();
        for (CalCalendarShareDO row : rows) {
            map.put(row.getCalendarId(), row.getPermission());
        }
        return map;
    }

    @Override
    public Set<Long> sharedReadCalendarIds(Long userId) {
        return sharedCalendarPermissions(userId).entrySet().stream()
                .filter(e -> PERMISSION_READ.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public List<CalCalendarDO> loadSharedCalendars(Long userId) {
        Set<Long> ids = sharedReadCalendarIds(userId);
        if (ids.isEmpty()) {
            return List.of();
        }
        return calendarMapper.selectBatchIds(ids);
    }

    private CalCalendarShareRespVO toResp(CalCalendarShareDO row, CalCalendarDO calendar, String direction) {
        CalCalendarShareRespVO vo = new CalCalendarShareRespVO();
        vo.setId(row.getId());
        vo.setCalendarId(row.getCalendarId());
        vo.setCalendarName(calendar != null ? calendar.getName() : "");
        vo.setCalendarColor(calendar != null ? calendar.getColor() : "#3B82F6");
        vo.setGranteeUserId(row.getGranteeUserId());
        vo.setGranteeNickname(resolveNickname(row.getGranteeUserId()));
        vo.setOwnerUserId(calendar != null ? calendar.getOwnerUserId() : null);
        vo.setOwnerNickname(calendar != null ? resolveNickname(calendar.getOwnerUserId()) : null);
        vo.setPermission(row.getPermission());
        vo.setDirection(direction);
        return vo;
    }

    private String resolveNickname(Long userId) {
        try {
            UserBasicDTO basic = userApi.getUserBasic(userId);
            if (basic != null && StringUtils.hasText(basic.getNickname())) {
                return basic.getNickname();
            }
            if (basic != null && StringUtils.hasText(basic.getUsername())) {
                return basic.getUsername();
            }
        } catch (Exception ex) {
            log.warn("Resolve nickname failed (best-effort): userId={}", userId, ex);
        }
        return String.valueOf(userId);
    }
}
