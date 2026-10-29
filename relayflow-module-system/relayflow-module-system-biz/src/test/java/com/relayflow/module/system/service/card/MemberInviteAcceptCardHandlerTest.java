package com.relayflow.module.system.service.card;

import com.relayflow.module.im.api.bot.ImBotApi;
import com.relayflow.module.im.api.card.CardActionContext;
import com.relayflow.module.im.api.card.CardActionResult;
import com.relayflow.module.system.dal.dataobject.SysTenantUserDO;
import com.relayflow.module.system.dal.mapper.SysTenantUserMapper;
import com.relayflow.module.system.dal.mapper.SysTenantUserExtMapper;
import com.relayflow.module.system.enums.TenantUserStatus;
import com.relayflow.module.system.service.tenant.TenantService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberInviteAcceptCardHandlerTest {

    @Mock
    private SysTenantUserMapper tenantUserMapper;
    @Mock
    private SysTenantUserExtMapper tenantUserExtMapper;
    @Mock
    private TenantService tenantService;
    @Mock
    private ImBotApi imBotApi;

    @InjectMocks
    private MemberInviteAcceptCardHandler handler;

    @Test
    void acceptActivatesPendingMembershipAndReturnsDoneCard() {
        SysTenantUserDO pending = new SysTenantUserDO();
        pending.setId(1L);
        pending.setTenantId(42L);
        pending.setUserId(200L);
        pending.setStatus(TenantUserStatus.NOT_JOINED);
        when(tenantUserMapper.selectOne(any())).thenReturn(pending);
        when(tenantUserExtMapper.updateStatus(42L, 200L, TenantUserStatus.ACTIVE, TenantUserStatus.NOT_JOINED)).thenReturn(1);

        CardActionResult result = handler.handle(CardActionContext.builder()
                .userId(200L)
                .actionKey(MemberInviteCardFactory.ACTION_KEY_ACCEPT)
                .payload(Map.of("tenantId", "42", "tenantName", "Acme"))
                .build());

        verify(tenantUserExtMapper).updateStatus(42L, 200L, TenantUserStatus.ACTIVE, TenantUserStatus.NOT_JOINED);
        verify(imBotApi).ensureUserEnablementsOnActive(42L, 200L);

        assertNotNull(result.getToast());
        assertEquals("success", result.getToast().getType());
        assertNotNull(result.getCard());
        assertEquals("已加入企业", ((Map<?, ?>) result.getCard().get("header")).get("title"));
    }
}
