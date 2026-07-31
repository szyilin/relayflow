package com.relayflow.module.system.service.contact;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.system.controller.app.vo.AppContactAccountSearchHitRespVO;
import com.relayflow.module.system.controller.app.vo.AppContactExternalItemRespVO;
import com.relayflow.module.system.controller.app.vo.AppContactRequestCreateReqVO;
import com.relayflow.module.system.controller.app.vo.AppContactRequestItemRespVO;
import com.relayflow.module.system.controller.app.vo.AppContactStarredItemRespVO;
import com.relayflow.module.system.controller.app.vo.AppContactStarredUpdateReqVO;
import com.relayflow.module.system.dal.dataobject.SysContactExternalDO;
import com.relayflow.module.system.dal.dataobject.SysContactRequestDO;
import com.relayflow.module.system.dal.dataobject.SysContactStarDO;
import com.relayflow.module.system.dal.dataobject.SysTenantDO;
import com.relayflow.module.system.dal.dataobject.SysTenantUserDO;
import com.relayflow.module.system.dal.dataobject.SysUserDO;
import com.relayflow.module.system.dal.mapper.SysContactExternalMapper;
import com.relayflow.module.system.dal.mapper.SysContactRequestMapper;
import com.relayflow.module.system.dal.mapper.SysContactStarMapper;
import com.relayflow.module.system.dal.mapper.SysTenantMapper;
import com.relayflow.module.system.dal.mapper.SysTenantUserMapper;
import com.relayflow.module.system.dal.mapper.SysUserMapper;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import com.relayflow.module.system.enums.TenantUserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Collator;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContactEnrichServiceImpl implements ContactEnrichService {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_ACCEPTED = "accepted";
    public static final String STATUS_REJECTED = "rejected";

    private static final int SUBTITLE_MAX = 200;

    private static final Collator ZH_COLLATOR = Collator.getInstance(Locale.CHINA);

    private final SysContactExternalMapper contactExternalMapper;
    private final SysContactRequestMapper contactRequestMapper;
    private final SysContactStarMapper contactStarMapper;
    private final SysUserMapper userMapper;
    private final SysTenantUserMapper tenantUserMapper;
    private final SysTenantMapper tenantMapper;

    @Override
    public List<AppContactExternalItemRespVO> listExternal() {
        Long ownerUserId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        requireActiveMember(tenantId, ownerUserId);

        List<SysContactExternalDO> rows = contactExternalMapper.selectList(new LambdaQueryWrapper<SysContactExternalDO>()
                .eq(SysContactExternalDO::getTenantId, tenantId)
                .eq(SysContactExternalDO::getOwnerUserId, ownerUserId));
        Map<Long, DisplayProfile> profiles = loadProfiles(
                rows.stream().map(SysContactExternalDO::getTargetUserId).collect(Collectors.toSet()),
                tenantId);

        return rows.stream()
                .map(row -> {
                    DisplayProfile profile = profiles.getOrDefault(row.getTargetUserId(), DisplayProfile.unknown());
                    AppContactExternalItemRespVO vo = new AppContactExternalItemRespVO();
                    vo.setId(row.getId());
                    vo.setUserId(row.getTargetUserId());
                    vo.setDisplayName(profile.displayName());
                    vo.setOrgLabel(profile.orgLabel());
                    vo.setAvatarText(profile.avatarText());
                    vo.setAvatar(profile.avatar());
                    vo.setSortLetter(sortLetter(profile.displayName()));
                    return vo;
                })
                .sorted(Comparator
                        .comparing(AppContactExternalItemRespVO::getSortLetter)
                        .thenComparing(AppContactExternalItemRespVO::getDisplayName, ZH_COLLATOR))
                .toList();
    }

    @Override
    public List<AppContactRequestItemRespVO> listRequests() {
        Long toUserId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        requireActiveMember(tenantId, toUserId);

        List<SysContactRequestDO> rows = contactRequestMapper.selectList(new LambdaQueryWrapper<SysContactRequestDO>()
                .eq(SysContactRequestDO::getTenantId, tenantId)
                .eq(SysContactRequestDO::getToUserId, toUserId)
                .orderByDesc(SysContactRequestDO::getCreateTime));
        Map<Long, DisplayProfile> profiles = loadProfiles(
                rows.stream().map(SysContactRequestDO::getFromUserId).collect(Collectors.toSet()),
                tenantId);

        return rows.stream()
                .map(row -> toRequestVo(row, profiles.getOrDefault(row.getFromUserId(), DisplayProfile.unknown())))
                .toList();
    }

    @Override
    @Transactional
    public AppContactRequestItemRespVO acceptRequest(Long requestId) {
        Long toUserId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        requireActiveMember(tenantId, toUserId);

        SysContactRequestDO row = contactRequestMapper.selectById(requestId);
        if (row == null
                || !Objects.equals(row.getTenantId(), tenantId)
                || !Objects.equals(row.getToUserId(), toUserId)) {
            throw new ServiceException(ErrorCodeConstants.CONTACT_REQUEST_NOT_FOUND);
        }
        if (!STATUS_PENDING.equals(row.getStatus())) {
            throw new ServiceException(ErrorCodeConstants.CONTACT_REQUEST_INVALID);
        }

        row.setStatus(STATUS_ACCEPTED);
        contactRequestMapper.updateById(row);
        upsertExternal(tenantId, toUserId, row.getFromUserId());

        DisplayProfile profile = loadProfiles(Set.of(row.getFromUserId()), tenantId)
                .getOrDefault(row.getFromUserId(), DisplayProfile.unknown());
        return toRequestVo(row, profile);
    }

    @Override
    @Transactional
    public AppContactRequestItemRespVO rejectRequest(Long requestId) {
        Long toUserId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        requireActiveMember(tenantId, toUserId);

        SysContactRequestDO row = contactRequestMapper.selectById(requestId);
        if (row == null
                || !Objects.equals(row.getTenantId(), tenantId)
                || !Objects.equals(row.getToUserId(), toUserId)) {
            throw new ServiceException(ErrorCodeConstants.CONTACT_REQUEST_NOT_FOUND);
        }
        if (!STATUS_PENDING.equals(row.getStatus())) {
            throw new ServiceException(ErrorCodeConstants.CONTACT_REQUEST_INVALID);
        }

        row.setStatus(STATUS_REJECTED);
        contactRequestMapper.updateById(row);

        DisplayProfile profile = loadProfiles(Set.of(row.getFromUserId()), tenantId)
                .getOrDefault(row.getFromUserId(), DisplayProfile.unknown());
        return toRequestVo(row, profile);
    }

    @Override
    public List<AppContactAccountSearchHitRespVO> searchAccount(String mobile, String email) {
        Long ownerUserId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        requireActiveMember(tenantId, ownerUserId);

        String mobileQ = StringUtils.hasText(mobile) ? mobile.trim() : null;
        String emailQ = StringUtils.hasText(email) ? email.trim() : null;
        if (mobileQ == null && emailQ == null) {
            throw new ServiceException(ErrorCodeConstants.CONTACT_ACCOUNT_SEARCH_INVALID);
        }
        if (mobileQ != null && emailQ != null) {
            throw new ServiceException(ErrorCodeConstants.CONTACT_ACCOUNT_SEARCH_INVALID);
        }
        if (mobileQ != null && !mobileQ.matches("^\\d{11}$")) {
            throw new ServiceException(ErrorCodeConstants.CONTACT_ACCOUNT_SEARCH_INVALID);
        }
        if (emailQ != null && (!emailQ.contains("@") || emailQ.length() < 5)) {
            throw new ServiceException(ErrorCodeConstants.CONTACT_ACCOUNT_SEARCH_INVALID);
        }

        LambdaQueryWrapper<SysUserDO> query = new LambdaQueryWrapper<SysUserDO>();
        if (mobileQ != null) {
            query.eq(SysUserDO::getMobile, mobileQ);
        } else {
            query.eq(SysUserDO::getEmail, emailQ);
        }
        List<SysUserDO> users = userMapper.selectList(query).stream()
                .filter(u -> !Objects.equals(u.getId(), ownerUserId))
                .toList();
        if (users.isEmpty()) {
            return List.of();
        }

        Set<Long> userIds = users.stream().map(SysUserDO::getId).collect(Collectors.toSet());
        Map<Long, DisplayProfile> profiles = loadProfiles(userIds, tenantId);
        Set<Long> related = loadAlreadyRelatedTargetIds(tenantId, ownerUserId, userIds);

        return users.stream()
                .map(user -> {
                    DisplayProfile profile = profiles.getOrDefault(user.getId(), DisplayProfile.unknown());
                    AppContactAccountSearchHitRespVO vo = new AppContactAccountSearchHitRespVO();
                    vo.setUserId(user.getId());
                    vo.setDisplayName(profile.displayName());
                    vo.setOrgLabel(profile.orgLabel());
                    vo.setAvatarText(profile.avatarText());
                    vo.setAvatar(profile.avatar());
                    vo.setAlreadyAdded(related.contains(user.getId()));
                    return vo;
                })
                .toList();
    }

    @Override
    @Transactional
    public AppContactRequestItemRespVO createRequest(AppContactRequestCreateReqVO request) {
        Long fromUserId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        requireActiveMember(tenantId, fromUserId);

        Long targetUserId = request.getTargetUserId();
        if (targetUserId == null) {
            throw new ServiceException(ErrorCodeConstants.USER_NOT_FOUND);
        }
        if (Objects.equals(targetUserId, fromUserId)) {
            throw new ServiceException(ErrorCodeConstants.CONTACT_REQUEST_SELF);
        }
        SysUserDO target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw new ServiceException(ErrorCodeConstants.USER_NOT_FOUND);
        }
        if (isAlreadyRelated(tenantId, fromUserId, targetUserId)) {
            throw new ServiceException(ErrorCodeConstants.CONTACT_REQUEST_DUPLICATE);
        }

        String subtitle = request.getMessage() == null ? "" : request.getMessage().trim();
        if (subtitle.length() > SUBTITLE_MAX) {
            subtitle = subtitle.substring(0, SUBTITLE_MAX);
        }

        SysContactRequestDO row = new SysContactRequestDO();
        row.setTenantId(tenantId);
        row.setFromUserId(fromUserId);
        row.setToUserId(targetUserId);
        row.setStatus(STATUS_PENDING);
        row.setSubtitle(subtitle);
        contactRequestMapper.insert(row);

        if (row.getCreateTime() == null) {
            SysContactRequestDO saved = contactRequestMapper.selectById(row.getId());
            if (saved != null) {
                row.setCreateTime(saved.getCreateTime());
            }
        }

        DisplayProfile profile = loadProfiles(Set.of(fromUserId), tenantId)
                .getOrDefault(fromUserId, DisplayProfile.unknown());
        return toRequestVo(row, profile);
    }

    @Override
    public List<AppContactStarredItemRespVO> listStarred() {
        Long ownerUserId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        requireActiveMember(tenantId, ownerUserId);

        List<SysContactStarDO> rows = contactStarMapper.selectList(new LambdaQueryWrapper<SysContactStarDO>()
                .eq(SysContactStarDO::getTenantId, tenantId)
                .eq(SysContactStarDO::getOwnerUserId, ownerUserId)
                .orderByDesc(SysContactStarDO::getCreateTime));
        Map<Long, DisplayProfile> profiles = loadProfiles(
                rows.stream().map(SysContactStarDO::getTargetUserId).collect(Collectors.toSet()),
                tenantId);

        return rows.stream()
                .map(row -> {
                    DisplayProfile profile = profiles.getOrDefault(row.getTargetUserId(), DisplayProfile.unknown());
                    AppContactStarredItemRespVO vo = new AppContactStarredItemRespVO();
                    vo.setId(row.getId());
                    vo.setUserId(row.getTargetUserId());
                    vo.setDisplayName(profile.displayName());
                    vo.setOrgLabel(profile.orgLabel());
                    vo.setAvatarText(profile.avatarText());
                    vo.setAvatar(profile.avatar());
                    vo.setUsername(profile.username());
                    return vo;
                })
                .toList();
    }

    @Override
    @Transactional
    public void updateStarred(Long userId, AppContactStarredUpdateReqVO request) {
        Long ownerUserId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        requireActiveMember(tenantId, ownerUserId);
        if (userId == null) {
            throw new ServiceException(ErrorCodeConstants.USER_NOT_FOUND);
        }

        boolean starred = Boolean.TRUE.equals(request.getStarred());
        SysContactStarDO existing = findStar(tenantId, ownerUserId, userId);
        if (starred) {
            if (existing == null) {
                SysContactStarDO row = new SysContactStarDO();
                row.setTenantId(tenantId);
                row.setOwnerUserId(ownerUserId);
                row.setTargetUserId(userId);
                contactStarMapper.insert(row);
            }
            return;
        }
        if (existing != null) {
            contactStarMapper.deleteById(existing.getId());
        }
    }

    private void upsertExternal(Long tenantId, Long ownerUserId, Long targetUserId) {
        SysContactExternalDO existing = contactExternalMapper.selectOne(new LambdaQueryWrapper<SysContactExternalDO>()
                .eq(SysContactExternalDO::getTenantId, tenantId)
                .eq(SysContactExternalDO::getOwnerUserId, ownerUserId)
                .eq(SysContactExternalDO::getTargetUserId, targetUserId)
                .last("LIMIT 1"));
        if (existing != null) {
            return;
        }
        SysContactExternalDO row = new SysContactExternalDO();
        row.setTenantId(tenantId);
        row.setOwnerUserId(ownerUserId);
        row.setTargetUserId(targetUserId);
        contactExternalMapper.insert(row);
    }

    private boolean isAlreadyRelated(Long tenantId, Long ownerUserId, Long targetUserId) {
        return loadAlreadyRelatedTargetIds(tenantId, ownerUserId, Set.of(targetUserId)).contains(targetUserId);
    }

    private Set<Long> loadAlreadyRelatedTargetIds(Long tenantId, Long ownerUserId, Set<Long> targetUserIds) {
        if (targetUserIds == null || targetUserIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> related = contactExternalMapper.selectList(new LambdaQueryWrapper<SysContactExternalDO>()
                        .eq(SysContactExternalDO::getTenantId, tenantId)
                        .eq(SysContactExternalDO::getOwnerUserId, ownerUserId)
                        .in(SysContactExternalDO::getTargetUserId, targetUserIds))
                .stream()
                .map(SysContactExternalDO::getTargetUserId)
                .collect(Collectors.toSet());
        related.addAll(contactRequestMapper.selectList(new LambdaQueryWrapper<SysContactRequestDO>()
                        .eq(SysContactRequestDO::getTenantId, tenantId)
                        .eq(SysContactRequestDO::getFromUserId, ownerUserId)
                        .eq(SysContactRequestDO::getStatus, STATUS_PENDING)
                        .in(SysContactRequestDO::getToUserId, targetUserIds))
                .stream()
                .map(SysContactRequestDO::getToUserId)
                .collect(Collectors.toSet()));
        return related;
    }

    private SysContactStarDO findStar(Long tenantId, Long ownerUserId, Long targetUserId) {
        return contactStarMapper.selectOne(new LambdaQueryWrapper<SysContactStarDO>()
                .eq(SysContactStarDO::getTenantId, tenantId)
                .eq(SysContactStarDO::getOwnerUserId, ownerUserId)
                .eq(SysContactStarDO::getTargetUserId, targetUserId)
                .last("LIMIT 1"));
    }

    private void requireActiveMember(Long tenantId, Long userId) {
        SysTenantUserDO tenantUser = tenantUserMapper.selectOne(new LambdaQueryWrapper<SysTenantUserDO>()
                .eq(SysTenantUserDO::getTenantId, tenantId)
                .eq(SysTenantUserDO::getUserId, userId)
                .eq(SysTenantUserDO::getStatus, TenantUserStatus.ACTIVE)
                .last("LIMIT 1"));
        if (tenantUser == null) {
            throw new ServiceException(ErrorCodeConstants.USER_NOT_FOUND);
        }
    }

    private Map<Long, DisplayProfile> loadProfiles(Set<Long> userIds, Long currentTenantId) {
        Map<Long, DisplayProfile> result = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return result;
        }
        List<Long> ids = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return result;
        }

        Map<Long, SysUserDO> users = userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUserDO::getId, u -> u, (a, b) -> a));
        List<SysTenantUserDO> memberships = tenantUserMapper.selectList(new LambdaQueryWrapper<SysTenantUserDO>()
                .in(SysTenantUserDO::getUserId, ids)
                .eq(SysTenantUserDO::getStatus, TenantUserStatus.ACTIVE));
        Map<Long, List<SysTenantUserDO>> byUser = memberships.stream()
                .collect(Collectors.groupingBy(SysTenantUserDO::getUserId));

        Set<Long> tenantIds = memberships.stream()
                .map(SysTenantUserDO::getTenantId)
                .collect(Collectors.toSet());
        Map<Long, SysTenantDO> tenants = tenantIds.isEmpty()
                ? Map.of()
                : tenantMapper.selectBatchIds(tenantIds).stream()
                        .collect(Collectors.toMap(SysTenantDO::getId, t -> t, (a, b) -> a));

        for (Long userId : ids) {
            SysUserDO user = users.get(userId);
            if (user == null) {
                result.put(userId, DisplayProfile.unknown());
                continue;
            }
            List<SysTenantUserDO> userMemberships = byUser.getOrDefault(userId, List.of());
            SysTenantUserDO preferred = userMemberships.stream()
                    .filter(m -> !Objects.equals(m.getTenantId(), currentTenantId))
                    .findFirst()
                    .orElse(userMemberships.stream().findFirst().orElse(null));

            String displayName;
            if (preferred != null && StringUtils.hasText(preferred.getNickname())) {
                displayName = preferred.getNickname().trim();
            } else if (StringUtils.hasText(user.getNickname())) {
                displayName = user.getNickname().trim();
            } else {
                displayName = user.getUsername();
            }

            String orgLabel = "";
            if (preferred != null) {
                SysTenantDO tenant = tenants.get(preferred.getTenantId());
                if (tenant != null && StringUtils.hasText(tenant.getName())) {
                    orgLabel = tenant.getName().trim();
                }
            }

            result.put(userId, new DisplayProfile(
                    displayName,
                    orgLabel,
                    avatarText(displayName, user.getUsername()),
                    resolveAvatar(preferred, user),
                    user.getUsername()));
        }
        return result;
    }

    private static AppContactRequestItemRespVO toRequestVo(SysContactRequestDO row, DisplayProfile profile) {
        AppContactRequestItemRespVO vo = new AppContactRequestItemRespVO();
        vo.setId(row.getId());
        vo.setFromUserId(row.getFromUserId());
        vo.setDisplayName(profile.displayName());
        vo.setSubtitle(row.getSubtitle() == null ? "" : row.getSubtitle());
        vo.setAvatarText(profile.avatarText());
        vo.setAvatar(profile.avatar());
        vo.setStatus(row.getStatus());
        vo.setCreateTime(row.getCreateTime());
        return vo;
    }

    private static String avatarText(String displayName, String username) {
        String source = StringUtils.hasText(displayName) ? displayName.trim() : username;
        if (!StringUtils.hasText(source)) {
            return "?";
        }
        return source.substring(0, 1);
    }

    private static String resolveAvatar(SysTenantUserDO member, SysUserDO user) {
        if (member != null && StringUtils.hasText(member.getAvatar())) {
            return member.getAvatar().trim();
        }
        if (user != null && StringUtils.hasText(user.getAvatar())) {
            return user.getAvatar().trim();
        }
        return null;
    }

    static String sortLetter(String displayName) {
        if (!StringUtils.hasText(displayName)) {
            return "#";
        }
        char c = Character.toUpperCase(displayName.trim().charAt(0));
        if (c >= 'A' && c <= 'Z') {
            return String.valueOf(c);
        }
        return "#";
    }

    private record DisplayProfile(
            String displayName,
            String orgLabel,
            String avatarText,
            String avatar,
            String username
    ) {
        static DisplayProfile unknown() {
            return new DisplayProfile("未知用户", "", "?", null, "");
        }
    }
}
