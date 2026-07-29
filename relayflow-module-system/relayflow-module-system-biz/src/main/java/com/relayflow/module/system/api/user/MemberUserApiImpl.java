package com.relayflow.module.system.api.user;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.util.MobileUtils;
import com.relayflow.module.system.api.user.dto.MemberSearchRespDTO;
import com.relayflow.module.system.dal.dataobject.SysDeptDO;
import com.relayflow.module.system.dal.dataobject.SysTenantUserDO;
import com.relayflow.module.system.dal.dataobject.SysUserDO;
import com.relayflow.module.system.dal.dataobject.SysUserDeptDO;
import com.relayflow.module.system.dal.mapper.SysDeptMapper;
import com.relayflow.module.system.dal.mapper.SysTenantUserMapper;
import com.relayflow.module.system.dal.mapper.SysUserDeptMapper;
import com.relayflow.module.system.dal.mapper.SysUserMapper;
import com.relayflow.module.system.enums.TenantUserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberUserApiImpl implements MemberUserApi {

    private static final int MAX_LIMIT = 10;

    private final SysTenantUserMapper tenantUserMapper;
    private final SysUserMapper userMapper;
    private final SysUserDeptMapper userDeptMapper;
    private final SysDeptMapper deptMapper;

    @Override
    public List<MemberSearchRespDTO> searchMembers(Long tenantId, String keyword, int limit) {
        if (tenantId == null || !StringUtils.hasText(keyword)) {
            return List.of();
        }
        int safeLimit = clampLimit(limit);
        String trimmed = keyword.trim();

        Set<Long> activeUserIds = tenantUserMapper.selectList(
                        Wrappers.<SysTenantUserDO>lambdaQuery()
                                .eq(SysTenantUserDO::getTenantId, tenantId)
                                .eq(SysTenantUserDO::getStatus, TenantUserStatus.ACTIVE))
                .stream()
                .map(SysTenantUserDO::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (activeUserIds.isEmpty()) {
            return List.of();
        }

        Map<Long, SysUserDO> matchedUsers = new LinkedHashMap<>();
        Long exactUserId = parseUserId(trimmed);
        if (exactUserId != null && activeUserIds.contains(exactUserId)) {
            SysUserDO exactUser = userMapper.selectById(exactUserId);
            if (exactUser != null) {
                matchedUsers.put(exactUser.getId(), exactUser);
            }
        }

        String normalizedMobile = normalizeMobile(trimmed);
        List<SysUserDO> keywordMatches = userMapper.selectList(
                Wrappers.<SysUserDO>lambdaQuery()
                        .in(SysUserDO::getId, activeUserIds)
                        .and(wrapper -> {
                            wrapper.like(SysUserDO::getNickname, trimmed)
                                    .or()
                                    .like(SysUserDO::getUsername, trimmed)
                                    .or()
                                    .like(SysUserDO::getEmail, trimmed);
                            if (normalizedMobile != null) {
                                wrapper.or().like(SysUserDO::getMobile, normalizedMobile);
                            }
                        })
                        .orderByAsc(SysUserDO::getNickname)
                        .orderByAsc(SysUserDO::getUsername)
                        .last("LIMIT " + safeLimit));
        for (SysUserDO user : keywordMatches) {
            matchedUsers.putIfAbsent(user.getId(), user);
            if (matchedUsers.size() >= safeLimit) {
                break;
            }
        }

        if (matchedUsers.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = new ArrayList<>(matchedUsers.keySet()).subList(0, Math.min(safeLimit, matchedUsers.size()));
        Map<Long, String> deptNames = loadPrimaryDeptNames(tenantId, userIds);
        Map<Long, Long> deptIds = loadPrimaryDeptIds(tenantId, userIds);
        Map<Long, String> avatars = loadAvatars(tenantId, userIds);

        return userIds.stream()
                .map(matchedUsers::get)
                .filter(Objects::nonNull)
                .map(user -> toDto(user, deptNames.get(user.getId()), deptIds.get(user.getId()), avatars.get(user.getId())))
                .toList();
    }

    @Override
    public List<MemberSearchRespDTO> pageActiveMembers(Long tenantId, String keyword, int offset, int limit) {
        if (tenantId == null) {
            return List.of();
        }
        int safeOffset = Math.max(0, offset);
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        List<Long> activeUserIds = listActiveUserIds(tenantId);
        if (activeUserIds.isEmpty()) {
            return List.of();
        }
        List<SysUserDO> users = selectActiveUsers(activeUserIds, keyword, safeOffset, safeLimit);
        if (users.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = users.stream().map(SysUserDO::getId).toList();
        Map<Long, String> deptNames = loadPrimaryDeptNames(tenantId, userIds);
        Map<Long, Long> deptIds = loadPrimaryDeptIds(tenantId, userIds);
        Map<Long, String> avatars = loadAvatars(tenantId, userIds);
        return users.stream()
                .map(user -> toDto(user, deptNames.get(user.getId()), deptIds.get(user.getId()), avatars.get(user.getId())))
                .toList();
    }

    @Override
    public long countActiveMembers(Long tenantId, String keyword) {
        if (tenantId == null) {
            return 0L;
        }
        List<Long> activeUserIds = listActiveUserIds(tenantId);
        if (activeUserIds.isEmpty()) {
            return 0L;
        }
        if (!StringUtils.hasText(keyword)) {
            return activeUserIds.size();
        }
        var query = Wrappers.<SysUserDO>lambdaQuery().in(SysUserDO::getId, activeUserIds);
        applyKeyword(query, keyword.trim());
        Long count = userMapper.selectCount(query);
        return count == null ? 0L : count;
    }

    @Override
    public Long getPrimaryDeptId(Long tenantId, Long userId) {
        if (tenantId == null || userId == null) {
            return null;
        }
        return loadPrimaryDeptIds(tenantId, List.of(userId)).get(userId);
    }

    private List<Long> listActiveUserIds(Long tenantId) {
        return tenantUserMapper.selectList(
                        Wrappers.<SysTenantUserDO>lambdaQuery()
                                .eq(SysTenantUserDO::getTenantId, tenantId)
                                .eq(SysTenantUserDO::getStatus, TenantUserStatus.ACTIVE))
                .stream()
                .map(SysTenantUserDO::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<SysUserDO> selectActiveUsers(List<Long> activeUserIds, String keyword, int offset, int limit) {
        var query = Wrappers.<SysUserDO>lambdaQuery()
                .in(SysUserDO::getId, activeUserIds)
                .orderByAsc(SysUserDO::getNickname)
                .orderByAsc(SysUserDO::getUsername);
        if (StringUtils.hasText(keyword)) {
            applyKeyword(query, keyword.trim());
        }
        query.last("LIMIT " + limit + " OFFSET " + offset);
        return userMapper.selectList(query);
    }

    private void applyKeyword(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUserDO> query, String trimmed) {
        String normalizedMobile = normalizeMobile(trimmed);
        Long exactUserId = parseUserId(trimmed);
        query.and(wrapper -> {
            wrapper.like(SysUserDO::getNickname, trimmed)
                    .or()
                    .like(SysUserDO::getUsername, trimmed)
                    .or()
                    .like(SysUserDO::getEmail, trimmed);
            if (normalizedMobile != null) {
                wrapper.or().like(SysUserDO::getMobile, normalizedMobile);
            }
            if (exactUserId != null) {
                wrapper.or().eq(SysUserDO::getId, exactUserId);
            }
        });
    }

    private MemberSearchRespDTO toDto(SysUserDO user, String deptName, Long deptId, String avatarFileId) {
        MemberSearchRespDTO dto = new MemberSearchRespDTO();
        dto.setUserId(user.getId());
        dto.setNickname(StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
        dto.setDeptName(deptName);
        dto.setDeptId(deptId);
        dto.setAvatarFileId(StringUtils.hasText(avatarFileId) ? avatarFileId.trim() : null);
        return dto;
    }

    private Map<Long, String> loadAvatars(Long tenantId, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> map = new LinkedHashMap<>();
        for (SysTenantUserDO row : tenantUserMapper.selectList(
                Wrappers.<SysTenantUserDO>lambdaQuery()
                        .eq(SysTenantUserDO::getTenantId, tenantId)
                        .in(SysTenantUserDO::getUserId, userIds))) {
            if (row.getUserId() != null && StringUtils.hasText(row.getAvatar())) {
                map.putIfAbsent(row.getUserId(), row.getAvatar().trim());
            }
        }
        return map;
    }

    private Map<Long, String> loadPrimaryDeptNames(Long tenantId, List<Long> userIds) {
        Map<Long, Long> deptIdByUserId = loadPrimaryDeptIds(tenantId, userIds);
        if (deptIdByUserId.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, SysDeptDO> deptById = deptMapper.selectBatchIds(deptIdByUserId.values()).stream()
                .collect(Collectors.toMap(SysDeptDO::getId, Function.identity()));
        return deptIdByUserId.entrySet().stream()
                .filter(entry -> deptById.containsKey(entry.getValue()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> deptById.get(entry.getValue()).getName()));
    }

    private Map<Long, Long> loadPrimaryDeptIds(Long tenantId, List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysUserDeptDO> relations = userDeptMapper.selectList(Wrappers.<SysUserDeptDO>lambdaQuery()
                .eq(SysUserDeptDO::getTenantId, tenantId)
                .in(SysUserDeptDO::getUserId, userIds));
        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }
        return relations.stream()
                .sorted(Comparator
                        .comparing(SysUserDeptDO::getPrimaryFlag, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(SysUserDeptDO::getId))
                .collect(Collectors.toMap(
                        SysUserDeptDO::getUserId,
                        SysUserDeptDO::getDeptId,
                        (existing, ignored) -> existing));
    }

    private Long parseUserId(String keyword) {
        if (!StringUtils.hasText(keyword) || !keyword.chars().allMatch(Character::isDigit)) {
            return null;
        }
        try {
            return Long.parseLong(keyword);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizeMobile(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        try {
            return MobileUtils.normalize(keyword);
        } catch (IllegalArgumentException ex) {
            return keyword.trim();
        }
    }

    private static int clampLimit(int limit) {
        if (limit <= 0) {
            return 5;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
