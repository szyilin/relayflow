package com.relayflow.module.infra.service.notify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.infra.api.notify.dto.NotifyItemCommand;
import com.relayflow.module.infra.api.realtime.RealtimeEventPublisher;
import com.relayflow.module.infra.api.realtime.dto.RealtimeEventDTO;
import com.relayflow.module.infra.api.realtime.enums.RealtimeDomain;
import com.relayflow.module.infra.dal.dataobject.InfraNotifyDO;
import com.relayflow.module.infra.dal.mapper.InfraNotifyMapper;
import com.relayflow.module.infra.dal.mapper.InfraNotifyPublicMapper;
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
import static org.mockito.Mockito.lenient;
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
    @Mock
    private RealtimeEventPublisher realtimeEventPublisher;

    @InjectMocks
    private NotifyInboxServiceImpl notifyInboxService;

    @BeforeEach
    void setUp() {
        notifyInboxService = new NotifyInboxServiceImpl(
                notifyMapper, notifyPublicMapper, new ObjectMapper(), realtimeEventPublisher);
        lenient().when(notifyPublicMapper.countUnreadByUserId(any())).thenReturn(0L);
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
        when(notifyPublicMapper.countByUserId(USER_ID, null)).thenReturn(1L);
        when(notifyPublicMapper.selectPageByUserId(USER_ID, null, 20, 0)).thenReturn(List.of(row));

        var page = notifyInboxService.pageByUserId(USER_ID, null, 1, 20);

        assertEquals(1, page.getTotal());
        assertEquals(1, page.getList().size());
    }

    @Test
    void pageByUserIdFiltersByType() {
        when(notifyPublicMapper.countByUserId(USER_ID, InfraNotifyType.TASK_DUE)).thenReturn(0L);
        when(notifyPublicMapper.selectPageByUserId(USER_ID, InfraNotifyType.TASK_DUE, 20, 0)).thenReturn(List.of());

        var page = notifyInboxService.pageByUserId(USER_ID, InfraNotifyType.TASK_DUE, 1, 20);

        assertEquals(0, page.getTotal());
        verify(notifyPublicMapper).countByUserId(USER_ID, InfraNotifyType.TASK_DUE);
    }

    @Test
    void markAllReadByUserIdDelegatesToPublicMapper() {
        notifyInboxService.markAllReadByUserId(USER_ID, InfraNotifyType.TASK_DUE);
        verify(notifyPublicMapper).markAllReadByUserId(eq(USER_ID), eq(InfraNotifyType.TASK_DUE), any());
    }

    @Test
    void pushPublishesNotifyWebSocketWhenUserIdPresent() {
        when(notifyMapper.selectOne(any())).thenReturn(null);
        when(notifyMapper.insert(any(InfraNotifyDO.class))).thenAnswer(invocation -> {
            InfraNotifyDO row = invocation.getArgument(0);
            row.setId(9001L);
            return 1;
        });
        when(notifyPublicMapper.countUnreadByUserId(USER_ID)).thenReturn(2L);

        notifyInboxService.push(taskDueCommand("task:100"));

        ArgumentCaptor<RealtimeEventDTO> captor = ArgumentCaptor.forClass(RealtimeEventDTO.class);
        verify(realtimeEventPublisher).publish(captor.capture());
        RealtimeEventDTO event = captor.getValue();
        assertEquals(RealtimeDomain.NOTIFY.getCode(), event.domain());
        assertEquals("notify.new", event.type());
        assertEquals(TENANT_ID, event.tenantId());
        assertEquals(List.of(USER_ID), event.targetUserIds());
    }

    @Test
    void pushSkipsNotifyWebSocketWhenUserIdMissing() {
        when(notifyMapper.selectOne(any())).thenReturn(null);
        when(notifyMapper.insert(any(InfraNotifyDO.class))).thenAnswer(invocation -> {
            InfraNotifyDO row = invocation.getArgument(0);
            row.setId(9001L);
            return 1;
        });

        notifyInboxService.push(inviteCommand(null, MOBILE));

        verify(realtimeEventPublisher, never()).publish(any());
    }

    @Test
    void markReadByIdsDelegatesToPublicMapper() {
        notifyInboxService.markReadByIds(USER_ID, List.of(10L, 11L));
        verify(notifyPublicMapper).markReadById(eq(10L), eq(USER_ID), any());
        verify(notifyPublicMapper).markReadById(eq(11L), eq(USER_ID), any());
    }

    @Test
    void pushWithDedupeKeyInsertsWhenNoDuplicate() {
        when(notifyMapper.selectOne(any())).thenReturn(null);
        when(notifyMapper.insert(any(InfraNotifyDO.class))).thenAnswer(invocation -> {
            InfraNotifyDO row = invocation.getArgument(0);
            row.setId(9002L);
            return 1;
        });

        NotifyItemCommand command = taskDueCommand("task:100");
        Long id = notifyInboxService.push(command);

        assertEquals(9002L, id);
        ArgumentCaptor<InfraNotifyDO> captor = ArgumentCaptor.forClass(InfraNotifyDO.class);
        verify(notifyMapper).insert(captor.capture());
        assertEquals("task:100", captor.getValue().getDedupeKey());
        assertEquals(InfraNotifyType.TASK_DUE, captor.getValue().getType());
    }

    @Test
    void pushWithDedupeKeyUpdatesUnreadDuplicate() {
        InfraNotifyDO existing = new InfraNotifyDO();
        existing.setId(44L);
        existing.setTenantId(TENANT_ID);
        existing.setUserId(USER_ID);
        existing.setType(InfraNotifyType.TASK_DUE);
        existing.setDedupeKey("task:100");
        existing.setReadFlag(0);
        existing.setTitle("旧提醒");
        when(notifyMapper.selectOne(any())).thenReturn(existing);

        NotifyItemCommand command = taskDueCommand("task:100");
        command.setTitle("新提醒");
        command.setBody("更新正文");

        Long id = notifyInboxService.push(command);

        assertEquals(44L, id);
        verify(notifyMapper).updateById(existing);
        verify(notifyMapper, never()).insert(any(InfraNotifyDO.class));
        assertEquals("新提醒", existing.getTitle());
        assertEquals("更新正文", existing.getBody());
    }

    @Test
    void pushWithDifferentDedupeKeysInsertsSeparately() {
        when(notifyMapper.selectOne(any())).thenReturn(null);
        when(notifyMapper.insert(any(InfraNotifyDO.class))).thenAnswer(invocation -> {
            InfraNotifyDO row = invocation.getArgument(0);
            row.setId(9003L);
            return 1;
        });

        notifyInboxService.push(taskDueCommand("task:100"));
        notifyInboxService.push(taskDueCommand("task:101"));

        verify(notifyMapper, org.mockito.Mockito.times(2)).insert(any(InfraNotifyDO.class));
    }

    private NotifyItemCommand taskDueCommand(String dedupeKey) {
        NotifyItemCommand command = new NotifyItemCommand();
        command.setTenantId(TENANT_ID);
        command.setUserId(USER_ID);
        command.setType(InfraNotifyType.TASK_DUE);
        command.setTitle("任务即将到期");
        command.setBody("「周报」将在明天到期");
        command.setDedupeKey(dedupeKey);
        command.setPayload(Map.of(
                "route", "/app/tasks?taskId=100",
                "entityType", "task",
                "entityId", "100"));
        return command;
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
