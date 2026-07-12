package com.relayflow.module.infra.service.notify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.infra.api.notify.dto.NotifyItemCommand;
import com.relayflow.module.infra.dal.dataobject.InfraNotifyDO;
import com.relayflow.module.infra.dal.mysql.InfraNotifyMapper;
import com.relayflow.module.infra.dal.mysql.InfraNotifyPublicMapper;
import com.relayflow.module.infra.enums.ErrorCodeConstants;
import com.relayflow.module.infra.enums.InfraNotifyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifyInboxServiceImplTest {

    private static final long TENANT_ID = 10L;
    private static final long USER_ID = 200L;
    private static final String MOBILE = "13900001111";

    @Mock
    private InfraNotifyMapper notifyMapper;
    @Mock
    private InfraNotifyPublicMapper notifyPublicMapper;

    @InjectMocks
    private NotifyInboxServiceImpl notifyInboxService;

    @BeforeEach
    void setUp() {
        notifyInboxService = new NotifyInboxServiceImpl(notifyMapper, notifyPublicMapper, new ObjectMapper());
    }

    @Test
    void pushInsertsWhenNoDuplicate() {
        when(notifyMapper.selectOne(any())).thenReturn(null);
        when(notifyMapper.insert(any(InfraNotifyDO.class))).thenAnswer(invocation -> {
            InfraNotifyDO row = invocation.getArgument(0);
            row.setId(9001L);
            return 1;
        });

        NotifyItemCommand command = inviteCommand(null, MOBILE);
        Long id = notifyInboxService.push(command);

        assertEquals(9001L, id);
        ArgumentCaptor<InfraNotifyDO> captor = ArgumentCaptor.forClass(InfraNotifyDO.class);
        verify(notifyMapper).insert(captor.capture());
        InfraNotifyDO inserted = captor.getValue();
        assertEquals(TENANT_ID, inserted.getTenantId());
        assertEquals(MOBILE, inserted.getMobile());
        assertEquals(InfraNotifyType.MEMBER_INVITE, inserted.getType());
        assertEquals(0, inserted.getReadFlag());
    }

    @Test
    void pushUpdatesUnreadDuplicateByMobile() {
        InfraNotifyDO existing = new InfraNotifyDO();
        existing.setId(42L);
        existing.setTenantId(TENANT_ID);
        existing.setMobile(MOBILE);
        existing.setType(InfraNotifyType.MEMBER_INVITE);
        existing.setReadFlag(0);
        existing.setTitle("旧标题");
        when(notifyMapper.selectOne(any())).thenReturn(existing);

        NotifyItemCommand command = inviteCommand(null, MOBILE);
        command.setTitle("新标题");
        command.setBody("新正文");

        Long id = notifyInboxService.push(command);

        assertEquals(42L, id);
        verify(notifyMapper).updateById(existing);
        verify(notifyMapper, never()).insert(any(InfraNotifyDO.class));
        assertEquals("新标题", existing.getTitle());
        assertEquals("新正文", existing.getBody());
    }

    @Test
    void pushUpdatesUnreadDuplicateByUserId() {
        InfraNotifyDO existing = new InfraNotifyDO();
        existing.setId(43L);
        existing.setTenantId(TENANT_ID);
        existing.setUserId(USER_ID);
        existing.setType(InfraNotifyType.MEMBER_INVITE);
        existing.setReadFlag(0);
        when(notifyMapper.selectOne(any())).thenReturn(existing);

        NotifyItemCommand command = inviteCommand(USER_ID, MOBILE);
        Long id = notifyInboxService.push(command);

        assertEquals(43L, id);
        verify(notifyMapper).updateById(existing);
        assertEquals(MOBILE, existing.getMobile());
    }

    @Test
    void pushRejectsMissingReceiver() {
        NotifyItemCommand command = inviteCommand(null, null);
        ServiceException ex = assertThrows(ServiceException.class, () -> notifyInboxService.push(command));
        assertEquals(ErrorCodeConstants.NOTIFY_RECEIVER_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    void listUnreadByMobileDelegatesToPublicMapper() {
        InfraNotifyDO row = new InfraNotifyDO();
        row.setId(1L);
        when(notifyPublicMapper.selectUnreadByMobile(MOBILE)).thenReturn(List.of(row));

        List<InfraNotifyDO> rows = notifyInboxService.listUnreadByMobile(MOBILE);

        assertEquals(1, rows.size());
        verify(notifyPublicMapper).selectUnreadByMobile(MOBILE);
    }

    @Test
    void backfillUserIdByMobileDelegatesToPublicMapper() {
        notifyInboxService.backfillUserIdByMobile(MOBILE, USER_ID);
        verify(notifyPublicMapper).updateUserIdByMobile(any(), any(), any());
    }

    @Test
    void pageByUserIdReturnsPagedRows() {
        InfraNotifyDO row = new InfraNotifyDO();
        row.setId(1L);
        when(notifyPublicMapper.countByUserId(USER_ID)).thenReturn(1L);
        when(notifyPublicMapper.selectPageByUserId(USER_ID, 20, 0)).thenReturn(List.of(row));

        var page = notifyInboxService.pageByUserId(USER_ID, 1, 20);

        assertEquals(1, page.getTotal());
        assertEquals(1, page.getList().size());
    }

    @Test
    void markReadByIdsDelegatesToPublicMapper() {
        notifyInboxService.markReadByIds(USER_ID, List.of(10L, 11L));
        verify(notifyPublicMapper).markReadById(eq(10L), eq(USER_ID), any());
        verify(notifyPublicMapper).markReadById(eq(11L), eq(USER_ID), any());
    }

    private NotifyItemCommand inviteCommand(Long userId, String mobile) {
        NotifyItemCommand command = new NotifyItemCommand();
        command.setTenantId(TENANT_ID);
        command.setUserId(userId);
        command.setMobile(mobile);
        command.setType(InfraNotifyType.MEMBER_INVITE);
        command.setTitle("邀请加入");
        command.setBody("管理员邀请你加入企业");
        command.setPayload(Map.of("tenantName", "Acme"));
        return command;
    }
}
