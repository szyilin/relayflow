package com.relayflow.module.task.service.collab;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.module.system.api.tenant.TenantMemberApi;
import com.relayflow.module.system.api.user.UserApi;
import com.relayflow.module.system.api.user.dto.UserBasicDTO;
import com.relayflow.module.task.controller.app.vo.TaskAssignReqVO;
import com.relayflow.module.task.controller.app.vo.TaskCommentCreateReqVO;
import com.relayflow.module.task.dal.dataobject.TaskItemDO;
import com.relayflow.module.task.dal.mapper.TaskActivityMapper;
import com.relayflow.module.task.dal.mapper.TaskCommentMapper;
import com.relayflow.module.task.dal.mapper.TaskFollowerMapper;
import com.relayflow.module.task.dal.mapper.TaskItemMapper;
import com.relayflow.module.task.enums.ErrorCodeConstants;
import com.relayflow.module.task.service.access.TaskAccessService;
import com.relayflow.module.task.service.notify.TaskAssignNotifyService;
import com.relayflow.module.task.service.notify.TaskDueNotifyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskCollabServiceImplTest {

    private static final long USER_ID = 100L;
    private static final long TENANT_ID = 1L;
    private static final long TASK_ID = 2001L;
    private static final long ASSIGNEE_ID = 200L;

    @Mock
    private TaskItemMapper taskItemMapper;
    @Mock
    private TaskFollowerMapper taskFollowerMapper;
    @Mock
    private TaskCommentMapper taskCommentMapper;
    @Mock
    private TaskActivityMapper taskActivityMapper;
    @Mock
    private TaskAccessService taskAccessService;
    @Mock
    private TaskActivityRecorder taskActivityRecorder;
    @Mock
    private TaskAssignNotifyService taskAssignNotifyService;
    @Mock
    private TaskDueNotifyService taskDueNotifyService;
    @Mock
    private TenantMemberApi tenantMemberApi;
    @Mock
    private UserApi userApi;

    private TaskCollabServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TaskCollabServiceImpl(
                taskItemMapper,
                taskFollowerMapper,
                taskCommentMapper,
                taskActivityMapper,
                taskAccessService,
                taskActivityRecorder,
                taskAssignNotifyService,
                taskDueNotifyService,
                tenantMemberApi,
                userApi);
        LoginUser loginUser = new LoginUser(USER_ID, "u", TENANT_ID, "member", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void assign_rejectsNonMember() {
        TaskItemDO task = new TaskItemDO();
        task.setId(TASK_ID);
        task.setAssigneeId(USER_ID);
        task.setTitle("整理周报");
        when(taskAccessService.requireEditable(TASK_ID, USER_ID)).thenReturn(task);
        when(tenantMemberApi.filterActiveMemberUserIds(eq(TENANT_ID), any()))
                .thenReturn(Set.of());

        TaskAssignReqVO request = new TaskAssignReqVO();
        request.setId(TASK_ID);
        request.setAssigneeId(ASSIGNEE_ID);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.assign(request));
        assertEquals(ErrorCodeConstants.TASK_ASSIGNEE_NOT_MEMBER.getCode(), ex.getCode());
        verify(taskItemMapper, never()).updateById(any(TaskItemDO.class));
    }

    @Test
    void assign_updatesAssigneeAndNotifies() {
        TaskItemDO task = new TaskItemDO();
        task.setId(TASK_ID);
        task.setTenantId(TENANT_ID);
        task.setAssigneeId(USER_ID);
        task.setTitle("整理周报");
        when(taskAccessService.requireEditable(TASK_ID, USER_ID)).thenReturn(task);
        when(tenantMemberApi.filterActiveMemberUserIds(eq(TENANT_ID), any()))
                .thenReturn(Set.of(ASSIGNEE_ID));
        UserBasicDTO basic = new UserBasicDTO();
        basic.setNickname("李四");
        when(userApi.getUserBasic(ASSIGNEE_ID)).thenReturn(basic);

        TaskAssignReqVO request = new TaskAssignReqVO();
        request.setId(TASK_ID);
        request.setAssigneeId(ASSIGNEE_ID);

        service.assign(request);

        ArgumentCaptor<TaskItemDO> captor = ArgumentCaptor.forClass(TaskItemDO.class);
        verify(taskItemMapper).updateById(captor.capture());
        assertEquals(ASSIGNEE_ID, captor.getValue().getAssigneeId());
        verify(taskAssignNotifyService).notifyAssignee(any(TaskItemDO.class), eq(ASSIGNEE_ID));
        verify(taskActivityRecorder).record(any(TaskItemDO.class), eq(USER_ID), any(), any());
    }

    @Test
    void createComment_rejectsEmpty() {
        TaskCommentCreateReqVO request = new TaskCommentCreateReqVO();
        request.setTaskId(TASK_ID);
        request.setContent("   ");

        ServiceException ex = assertThrows(ServiceException.class, () -> service.createComment(request));
        assertEquals(ErrorCodeConstants.TASK_COMMENT_EMPTY.getCode(), ex.getCode());
    }
}
