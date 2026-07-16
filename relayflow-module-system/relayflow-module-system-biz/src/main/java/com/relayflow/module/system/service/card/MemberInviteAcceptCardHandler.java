package com.relayflow.module.system.service.card;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.im.api.bot.ImBotApi;
import com.relayflow.module.im.api.card.CardActionContext;
import com.relayflow.module.im.api.card.CardActionHandler;
import com.relayflow.module.im.api.card.CardActionResult;
import com.relayflow.module.system.dal.dataobject.SysTenantDO;
import com.relayflow.module.system.dal.dataobject.SysTenantUserDO;
import com.relayflow.module.system.dal.mapper.SysTenantUserMapper;
import com.relayflow.module.system.enums.ErrorCodeConstants;
import com.relayflow.module.system.enums.TenantUserStatus;
import com.relayflow.module.system.service.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Accepts a pending ({@code NOT_JOINED}) membership from an org-assistant invite card.
 */
@Component
@RequiredArgsConstructor
public class MemberInviteAcceptCardHandler implements CardActionHandler {

    private final SysTenantUserMapper tenantUserMapper;
    private final TenantService tenantService;
    /** Breaks CardHandler → ImBotApi → ConversationService → UserApi cycle. */
    @Lazy
    private final ImBotApi imBotApi;

    @Override
    public String actionKey() {
        return MemberInviteCardFactory.ACTION_KEY_ACCEPT;
    }

    @Override
    public CardActionResult handle(CardActionContext context) {
        Long invitingTenantId = parseTenantId(context.getPayload());
        if (invitingTenantId == null || context.getUserId() == null) {
            throw new ServiceException(ErrorCodeConstants.MEMBER_INVITE_NOT_FOUND);
        }

        SysTenantUserDO membership = tenantUserMapper.selectOne(Wrappers.<SysTenantUserDO>lambdaQuery()
                .eq(SysTenantUserDO::getUserId, context.getUserId())
                .eq(SysTenantUserDO::getTenantId, invitingTenantId));
        if (membership == null) {
            throw new ServiceException(ErrorCodeConstants.MEMBER_INVITE_NOT_FOUND);
        }

        String tenantName = resolveTenantName(invitingTenantId, context.getPayload());
        if (TenantUserStatus.ACTIVE.equals(membership.getStatus())) {
            return CardActionResult.builder()
                    .toast(CardActionResult.CardToast.builder()
                            .type("success")
                            .content("你已是该企业成员")
                            .build())
                    .card(MemberInviteCardFactory.accepted(tenantName))
                    .build();
        }
        if (!TenantUserStatus.NOT_JOINED.equals(membership.getStatus())) {
            throw new ServiceException(ErrorCodeConstants.MEMBER_INVITE_NOT_FOUND);
        }

        membership.setStatus(TenantUserStatus.ACTIVE);
        tenantUserMapper.updateById(membership);
        imBotApi.ensureUserEnablementsOnActive(invitingTenantId, context.getUserId());

        return CardActionResult.builder()
                .toast(CardActionResult.CardToast.builder()
                        .type("success")
                        .content("已加入「" + tenantName + "」，可在左下角切换企业")
                        .build())
                .card(MemberInviteCardFactory.accepted(tenantName))
                .build();
    }

    private String resolveTenantName(Long tenantId, Map<String, Object> payload) {
        if (payload != null) {
            Object name = payload.get("tenantName");
            if (name != null && StringUtils.hasText(String.valueOf(name))) {
                return String.valueOf(name).trim();
            }
        }
        SysTenantDO tenant = tenantService.getTenant(tenantId);
        return tenant != null && StringUtils.hasText(tenant.getName()) ? tenant.getName() : "企业";
    }

    private Long parseTenantId(Map<String, Object> payload) {
        if (payload == null || payload.get("tenantId") == null) {
            return null;
        }
        Object raw = payload.get("tenantId");
        if (raw instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(raw).trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
