package com.relayflow.module.system.service.invite;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.common.util.MobileUtils;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.im.api.bot.ImBotApi;
import com.relayflow.module.system.controller.admin.invite.vo.InviteSettingsRespVO;
import com.relayflow.module.system.controller.app.vo.AppOrgInviteCapabilityRespVO;
import com.relayflow.module.system.controller.app.vo.AppOrgInviteConsumeReqVO;
import com.relayflow.module.system.controller.app.vo.AppOrgInviteConsumeRespVO;
import com.relayflow.module.system.controller.app.vo.AppOrgInviteMobileCreateReqVO;
import com.relayflow.module.system.controller.app.vo.AppOrgInviteMobileRespVO;
import com.relayflow.module.system.controller.app.vo.AppOrgInviteSharedRespVO;
import com.relayflow.module.system.dal.dataobject.SysOrgInviteDO;
import com.relayflow.module.system.dal.dataobject.SysTenantUserDO;
import com.relayflow.module.system.dal.dataobject.SysUserDO;
import com.relayflow.module.system.dal.dataobject.SysUserDeptDO;
import com.relayflow.module.system.dal.mapper.SysOrgInviteMapper;
import com.relayflow.module.system.dal.mapper.SysOrgInvitePublicMapper;
import com.relayflow.module.system.dal.mapper.SysTenantUserMapper;
import com.relayflow.module.system.dal.mapper.SysUserDeptMapper;
import com.relayflow.module.system.dal.mapper.SysUserMapper;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import com.relayflow.module.system.enums.TenantUserStatus;
import com.relayflow.module.system.service.dept.DeptService;
import com.relayflow.module.system.service.permission.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrgInviteServiceImpl implements OrgInviteService {

    static final String CHANNEL_SHARED = "SHARED";
    static final String CHANNEL_MOBILE = "MOBILE";
    static final String STATUS_ACTIVE = "ACTIVE";
    static final String STATUS_PENDING = "PENDING_APPROVAL";
    static final String STATUS_CONSUMED = "CONSUMED";
    static final String STATUS_REVOKED = "REVOKED";
    static final String STATUS_EXPIRED = "EXPIRED";

    private static final String MODE_OFF = "OFF";
    private static final String MODE_ADMIN_ONLY = "ADMIN_ONLY";
    private static final String MODE_ALL_MEMBERS = "ALL_MEMBERS";
    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SysOrgInviteMapper orgInviteMapper;
    private final SysOrgInvitePublicMapper orgInvitePublicMapper;
    private final SysTenantUserMapper tenantUserMapper;
    private final SysUserDeptMapper userDeptMapper;
    private final SysUserMapper userMapper;
    private final InviteSettingsService inviteSettingsService;
    private final PermissionService permissionService;
    private final DeptService deptService;
    @Lazy
    private final ImBotApi imBotApi;

    @Override
    public AppOrgInviteCapabilityRespVO getCapability() {
        long tenantId = TenantContextHolder.getRequiredTenantId();
        long userId = SecurityFrameworkUtils.requireLoginUserId();
        InviteSettingsRespVO settings = inviteSettingsService.getSettings(tenantId);
        AppOrgInviteCapabilityRespVO resp = new AppOrgInviteCapabilityRespVO();
        resp.setInviteMode(settings.getInviteMode());
        resp.setCanInvite(resolveCanInvite(tenantId, userId, settings));
        return resp;
    }

    @Override
    @Transactional
    public AppOrgInviteSharedRespVO getOrCreateShared() {
        requireCanInvite();
        long tenantId = TenantContextHolder.getRequiredTenantId();
        SysOrgInviteDO existing = findActiveShared(tenantId);
        if (existing != null && !isExpired(existing)) {
            return toSharedResp(existing);
        }
        if (existing != null) {
            existing.setStatus(STATUS_EXPIRED);
            orgInviteMapper.updateById(existing);
        }
        return toSharedResp(createShared(tenantId));
    }

    @Override
    @Transactional
    public AppOrgInviteSharedRespVO resetShared() {
        requireCanInvite();
        long tenantId = TenantContextHolder.getRequiredTenantId();
        SysOrgInviteDO existing = findActiveShared(tenantId);
        if (existing != null) {
            existing.setStatus(STATUS_REVOKED);
            orgInviteMapper.updateById(existing);
        }
        return toSharedResp(createShared(tenantId));
    }

    @Override
    @Transactional
    public AppOrgInviteMobileRespVO createMobileInvite(AppOrgInviteMobileCreateReqVO request) {
        CapabilityGate gate = requireCanInvite();
        String mobile = MobileUtils.normalize(request.getMobile());
        if (!StringUtils.hasText(mobile) || !mobile.matches("^1\\d{10}$")) {
            throw new ServiceException(ErrorCodeConstants.ORG_INVITE_MOBILE_INVALID);
        }

        long tenantId = TenantContextHolder.getRequiredTenantId();
        long userId = SecurityFrameworkUtils.requireLoginUserId();
        InviteSettingsRespVO settings = gate.settings();

        SysOrgInviteDO row = new SysOrgInviteDO();
        row.setTenantId(tenantId);
        row.setChannel(CHANNEL_MOBILE);
        row.setInviterUserId(userId);
        row.setTargetMobile(mobile);
        row.setTargetRoleIds("[]");
        row.setExpiresAt(computeExpiresAt(settings));
        boolean needsApproval = !gate.admin()
                && Boolean.TRUE.equals(settings.getNonAdminRequireApproval());
        row.setStatus(needsApproval ? STATUS_PENDING : STATUS_ACTIVE);
        orgInviteMapper.insert(row);

        AppOrgInviteMobileRespVO resp = new AppOrgInviteMobileRespVO();
        resp.setId(String.valueOf(row.getId()));
        resp.setMobile(mobile);
        resp.setStatus(row.getStatus());
        resp.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC).toString());
        return resp;
    }

    @Override
    @Transactional
    public AppOrgInviteConsumeRespVO consume(AppOrgInviteConsumeReqVO request) {
        long userId = SecurityFrameworkUtils.requireLoginUserId();
        String token = trimToNull(request.getToken());
        String code = trimToNull(request.getCode());
        if (token == null && code == null) {
            throw new ServiceException(ErrorCodeConstants.ORG_INVITE_CONSUME_INVALID);
        }
        if (token != null && code != null) {
            throw new ServiceException(ErrorCodeConstants.ORG_INVITE_CONSUME_INVALID);
        }

        SysOrgInviteDO invite = token != null
                ? orgInvitePublicMapper.selectByTokenGlobal(token)
                : orgInvitePublicMapper.selectByInviteCodeGlobal(code.toUpperCase());
        if (invite == null) {
            throw new ServiceException(ErrorCodeConstants.ORG_INVITE_NOT_FOUND);
        }

        Long previousTenant = TenantContextHolder.get();
        try {
            TenantContextHolder.set(invite.getTenantId());
            return consumeInTenant(invite, userId);
        } finally {
            if (previousTenant == null) {
                TenantContextHolder.clear();
            } else {
                TenantContextHolder.set(previousTenant);
            }
        }
    }

    private AppOrgInviteConsumeRespVO consumeInTenant(SysOrgInviteDO invite, long userId) {
        long tenantId = invite.getTenantId();
        InviteSettingsRespVO settings = inviteSettingsService.getSettings(tenantId);
        if (MODE_OFF.equals(settings.getInviteMode())) {
            throw new ServiceException(ErrorCodeConstants.ORG_INVITE_CLOSED);
        }
        if (STATUS_REVOKED.equals(invite.getStatus()) || STATUS_EXPIRED.equals(invite.getStatus())) {
            throw new ServiceException(ErrorCodeConstants.ORG_INVITE_EXPIRED);
        }
        if (STATUS_PENDING.equals(invite.getStatus())) {
            throw new ServiceException(ErrorCodeConstants.ORG_INVITE_PENDING_APPROVAL);
        }
        if (STATUS_CONSUMED.equals(invite.getStatus())) {
            throw new ServiceException(ErrorCodeConstants.ORG_INVITE_EXPIRED);
        }
        if (!STATUS_ACTIVE.equals(invite.getStatus())) {
            throw new ServiceException(ErrorCodeConstants.ORG_INVITE_NOT_FOUND);
        }
        if (isExpired(invite)) {
            invite.setStatus(STATUS_EXPIRED);
            orgInviteMapper.updateById(invite);
            throw new ServiceException(ErrorCodeConstants.ORG_INVITE_EXPIRED);
        }

        SysTenantUserDO membership = tenantUserMapper.selectOne(Wrappers.<SysTenantUserDO>lambdaQuery()
                .eq(SysTenantUserDO::getTenantId, tenantId)
                .eq(SysTenantUserDO::getUserId, userId)
                .last("LIMIT 1"));

        boolean alreadyActive = membership != null
                && TenantUserStatus.ACTIVE.equals(membership.getStatus());
        if (!alreadyActive) {
            activateMembership(tenantId, userId, membership, invite.getTargetDeptId());
            imBotApi.ensureUserEnablementsOnActive(tenantId, userId);
        }

        if (CHANNEL_MOBILE.equals(invite.getChannel())) {
            invite.setStatus(STATUS_CONSUMED);
            invite.setConsumedAt(OffsetDateTime.now(ZoneOffset.UTC));
            invite.setMembershipUserId(userId);
            orgInviteMapper.updateById(invite);
        }

        AppOrgInviteConsumeRespVO resp = new AppOrgInviteConsumeRespVO();
        resp.setJoined(true);
        resp.setAlreadyMember(alreadyActive);
        resp.setTenantId(String.valueOf(tenantId));
        return resp;
    }

    private void activateMembership(long tenantId, long userId, SysTenantUserDO existing, Long targetDeptId) {
        SysUserDO user = userMapper.selectById(userId);
        String nickname = user != null && StringUtils.hasText(user.getNickname())
                ? user.getNickname()
                : (user != null ? user.getUsername() : String.valueOf(userId));

        if (existing == null) {
            SysTenantUserDO row = new SysTenantUserDO();
            row.setTenantId(tenantId);
            row.setUserId(userId);
            row.setStatus(TenantUserStatus.ACTIVE);
            row.setNickname(nickname);
            row.setSignature("");
            tenantUserMapper.insert(row);
        } else {
            existing.setStatus(TenantUserStatus.ACTIVE);
            if (!StringUtils.hasText(existing.getNickname())) {
                existing.setNickname(nickname);
            }
            tenantUserMapper.updateById(existing);
        }

        Long deptCount = userDeptMapper.selectCount(Wrappers.<SysUserDeptDO>lambdaQuery()
                .eq(SysUserDeptDO::getTenantId, tenantId)
                .eq(SysUserDeptDO::getUserId, userId));
        if (deptCount == null || deptCount == 0) {
            Long deptId = targetDeptId != null ? targetDeptId : deptService.getOrCreateRootDept(tenantId);
            SysUserDeptDO relation = new SysUserDeptDO();
            relation.setTenantId(tenantId);
            relation.setUserId(userId);
            relation.setDeptId(deptId);
            relation.setPrimaryFlag(1);
            userDeptMapper.insert(relation);
        }
    }

    private CapabilityGate requireCanInvite() {
        long tenantId = TenantContextHolder.getRequiredTenantId();
        long userId = SecurityFrameworkUtils.requireLoginUserId();
        InviteSettingsRespVO settings = inviteSettingsService.getSettings(tenantId);
        if (MODE_OFF.equals(settings.getInviteMode())) {
            throw new ServiceException(ErrorCodeConstants.ORG_INVITE_CLOSED);
        }
        boolean admin = permissionService.isAdmin(userId, tenantId);
        if (!resolveCanInvite(tenantId, userId, settings)) {
            throw new ServiceException(ErrorCodeConstants.ORG_INVITE_FORBIDDEN);
        }
        return new CapabilityGate(settings, admin);
    }

    private boolean resolveCanInvite(long tenantId, long userId, InviteSettingsRespVO settings) {
        String mode = settings.getInviteMode();
        if (MODE_OFF.equals(mode)) {
            return false;
        }
        if (MODE_ALL_MEMBERS.equals(mode)) {
            return true;
        }
        // ADMIN_ONLY (default)
        return permissionService.isAdmin(userId, tenantId);
    }

    private SysOrgInviteDO findActiveShared(long tenantId) {
        return orgInviteMapper.selectOne(new LambdaQueryWrapper<SysOrgInviteDO>()
                .eq(SysOrgInviteDO::getTenantId, tenantId)
                .eq(SysOrgInviteDO::getChannel, CHANNEL_SHARED)
                .eq(SysOrgInviteDO::getStatus, STATUS_ACTIVE)
                .orderByDesc(SysOrgInviteDO::getId)
                .last("LIMIT 1"));
    }

    private SysOrgInviteDO createShared(long tenantId) {
        long userId = SecurityFrameworkUtils.requireLoginUserId();
        InviteSettingsRespVO settings = inviteSettingsService.getSettings(tenantId);
        SysOrgInviteDO row = new SysOrgInviteDO();
        row.setTenantId(tenantId);
        row.setChannel(CHANNEL_SHARED);
        row.setToken(newToken());
        row.setInviteCode(newInviteCode());
        row.setInviterUserId(userId);
        row.setTargetRoleIds("[]");
        row.setStatus(STATUS_ACTIVE);
        row.setExpiresAt(computeExpiresAt(settings));
        orgInviteMapper.insert(row);
        return row;
    }

    private OffsetDateTime computeExpiresAt(InviteSettingsRespVO settings) {
        int days = settings.getNonAdminExpireDays() != null ? settings.getNonAdminExpireDays() : 3;
        return OffsetDateTime.now(ZoneOffset.UTC).plusDays(days);
    }

    private boolean isExpired(SysOrgInviteDO invite) {
        return invite.getExpiresAt() != null
                && invite.getExpiresAt().isBefore(OffsetDateTime.now(ZoneOffset.UTC));
    }

    private AppOrgInviteSharedRespVO toSharedResp(SysOrgInviteDO row) {
        AppOrgInviteSharedRespVO resp = new AppOrgInviteSharedRespVO();
        resp.setToken(row.getToken());
        resp.setInviteCode(row.getInviteCode());
        resp.setExpiresAt(row.getExpiresAt() != null ? row.getExpiresAt().toString() : null);
        return resp;
    }

    private static String newToken() {
        return "t_" + UUID.randomUUID().toString().replace("-", "");
    }

    private static String newInviteCode() {
        char[] buf = new char[6];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = CODE_ALPHABET[RANDOM.nextInt(CODE_ALPHABET.length)];
        }
        return new String(buf);
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private record CapabilityGate(InviteSettingsRespVO settings, boolean admin) {
    }
}
