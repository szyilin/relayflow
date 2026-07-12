package com.relayflow.module.system.service.memberinvite;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.util.MobileUtils;
import com.relayflow.module.system.controller.app.vo.MemberInvitePendingItemVO;
import com.relayflow.module.system.controller.app.vo.MemberInvitePendingListRespVO;
import com.relayflow.module.system.dal.dataobject.SysTenantDO;
import com.relayflow.module.system.dal.dataobject.SysTenantUserDO;
import com.relayflow.module.system.dal.dataobject.SysUserDO;
import com.relayflow.module.system.dal.mysql.SysTenantUserMapper;
import com.relayflow.module.system.dal.mysql.SysUserMapper;
import com.relayflow.module.system.enums.TenantUserStatus;
import com.relayflow.module.system.service.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberInvitePendingServiceImpl implements MemberInvitePendingService {

    private final SysUserMapper userMapper;
    private final SysTenantUserMapper tenantUserMapper;
    private final TenantService tenantService;

    @Override
    public MemberInvitePendingListRespVO listPendingByMobile(String mobile) {
        MemberInvitePendingListRespVO response = new MemberInvitePendingListRespVO();
        String normalizedMobile = MobileUtils.normalize(mobile);
        if (!StringUtils.hasText(normalizedMobile)) {
            return response;
        }

        SysUserDO user = userMapper.selectOne(Wrappers.<SysUserDO>lambdaQuery()
                .eq(SysUserDO::getMobile, normalizedMobile));
        if (user == null) {
            return response;
        }

        List<SysTenantUserDO> pendingMemberships = tenantUserMapper.selectList(
                Wrappers.<SysTenantUserDO>lambdaQuery()
                        .eq(SysTenantUserDO::getUserId, user.getId())
                        .eq(SysTenantUserDO::getStatus, TenantUserStatus.NOT_JOINED));
        List<MemberInvitePendingItemVO> items = pendingMemberships.stream()
                .sorted(Comparator.comparing(SysTenantUserDO::getCreateTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toPendingItem)
                .toList();
        response.setItems(items);
        return response;
    }

    private MemberInvitePendingItemVO toPendingItem(SysTenantUserDO membership) {
        SysTenantDO tenant = tenantService.getTenant(membership.getTenantId());
        MemberInvitePendingItemVO item = new MemberInvitePendingItemVO();
        item.setTenantId(tenant.getId());
        item.setTenantName(tenant.getName());
        item.setInvitedAt(membership.getCreateTime());
        return item;
    }
}
