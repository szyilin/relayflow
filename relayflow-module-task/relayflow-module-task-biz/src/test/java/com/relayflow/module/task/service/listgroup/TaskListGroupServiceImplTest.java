package com.relayflow.module.task.service.listgroup;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.module.task.controller.app.vo.TaskListGroupCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListGroupListRespVO;
import com.relayflow.module.task.controller.app.vo.TaskListGroupMoveReqVO;
import com.relayflow.module.task.dal.dataobject.TaskItemDO;
import com.relayflow.module.task.dal.dataobject.TaskListGroupDO;
import com.relayflow.module.task.dal.dataobject.TaskListItemDO;
import com.relayflow.module.task.dal.mapper.TaskListGroupMapper;
import com.relayflow.module.task.dal.mapper.TaskListItemMapper;
import com.relayflow.module.task.enums.ErrorCodeConstants;
import com.relayflow.module.task.service.access.TaskAccessService;
import com.relayflow.module.task.service.access.TaskListAccessService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskListGroupServiceImplTest {

    private static final long USER_ID = 100L;
    private static final long TENANT_ID = 1L;
    private static final long LIST_ID = 501L;
    private static final long DEFAULT_GROUP_ID = 10L;
    private static final long CUSTOM_GROUP_ID = 11L;
    private static final long TASK_ID = 2001L;

    @Mock
    private TaskListGroupMapper taskListGroupMapper;
    @Mock
    private TaskListItemMapper taskListItemMapper;
    @Mock
    private TaskListAccessService taskListAccessService;
    @Mock
    private TaskAccessService taskAccessService;

    private TaskListGroupServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TaskListGroupServiceImpl(
                taskListGroupMapper, taskListItemMapper, taskListAccessService, taskAccessService);
        LoginUser loginUser = new LoginUser(USER_ID, "u", TENANT_ID, "member", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void list_createsDefaultWhenMissing() {
        when(taskListGroupMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(taskListGroupMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(taskListItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        TaskListGroupListRespVO resp = service.list(LIST_ID);

        ArgumentCaptor<TaskListGroupDO> captor = ArgumentCaptor.forClass(TaskListGroupDO.class);
        verify(taskListGroupMapper).insert(captor.capture());
        assertEquals(1, captor.getValue().getIsDefault());
        assertEquals(LIST_ID, captor.getValue().getListId());
        assertEquals("默认", captor.getValue().getName());
        assertTrue(resp.getMemberships().isEmpty());
        verify(taskListAccessService).requireReadable(LIST_ID, USER_ID);
    }

    @Test
    void delete_defaultForbidden() {
        TaskListGroupDO def = group(DEFAULT_GROUP_ID, "默认", 1);
        when(taskListGroupMapper.selectById(DEFAULT_GROUP_ID)).thenReturn(def);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.delete(DEFAULT_GROUP_ID));
        assertEquals(ErrorCodeConstants.TASK_LIST_GROUP_FORBIDDEN.getCode(), ex.getCode());
        verify(taskListGroupMapper, never()).deleteById(eq(DEFAULT_GROUP_ID));
    }

    @Test
    void delete_movesMembershipsToDefault() {
        TaskListGroupDO custom = group(CUSTOM_GROUP_ID, "本周", 0);
        TaskListGroupDO def = group(DEFAULT_GROUP_ID, "默认", 1);
        when(taskListGroupMapper.selectById(CUSTOM_GROUP_ID)).thenReturn(custom);
        when(taskListGroupMapper.selectOne(any(Wrapper.class))).thenReturn(def);

        TaskListItemDO item = new TaskListItemDO();
        item.setId(99L);
        item.setListId(LIST_ID);
        item.setTaskId(TASK_ID);
        item.setGroupId(CUSTOM_GROUP_ID);
        when(taskListItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(item));

        service.delete(CUSTOM_GROUP_ID);

        ArgumentCaptor<TaskListItemDO> captor = ArgumentCaptor.forClass(TaskListItemDO.class);
        verify(taskListItemMapper).updateById(captor.capture());
        assertEquals(DEFAULT_GROUP_ID, captor.getValue().getGroupId());
        verify(taskListGroupMapper).deleteById(CUSTOM_GROUP_ID);
    }

    @Test
    void move_updatesMembershipGroup() {
        TaskListGroupDO custom = group(CUSTOM_GROUP_ID, "本周", 0);
        when(taskListGroupMapper.selectOne(any(Wrapper.class))).thenReturn(group(DEFAULT_GROUP_ID, "默认", 1));
        when(taskListGroupMapper.selectById(CUSTOM_GROUP_ID)).thenReturn(custom);
        when(taskAccessService.requireAccessible(eq(TASK_ID), eq(USER_ID))).thenReturn(new TaskItemDO());

        TaskListItemDO membership = new TaskListItemDO();
        membership.setId(88L);
        membership.setListId(LIST_ID);
        membership.setTaskId(TASK_ID);
        membership.setGroupId(DEFAULT_GROUP_ID);
        membership.setRank(0);
        when(taskListItemMapper.selectOne(any(Wrapper.class))).thenReturn(membership);
        when(taskListItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        TaskListGroupMoveReqVO req = new TaskListGroupMoveReqVO();
        req.setListId(LIST_ID);
        req.setTaskId(TASK_ID);
        req.setGroupId(CUSTOM_GROUP_ID);
        service.move(req);

        ArgumentCaptor<TaskListItemDO> captor = ArgumentCaptor.forClass(TaskListItemDO.class);
        verify(taskListItemMapper).updateById(captor.capture());
        assertEquals(CUSTOM_GROUP_ID, captor.getValue().getGroupId());
        verify(taskListAccessService).requireCanMutateTasks(LIST_ID, USER_ID);
    }

    @Test
    void create_rejectsBlankName() {
        when(taskListGroupMapper.selectOne(any(Wrapper.class))).thenReturn(group(DEFAULT_GROUP_ID, "默认", 1));
        TaskListGroupCreateReqVO req = new TaskListGroupCreateReqVO();
        req.setListId(LIST_ID);
        req.setName("   ");
        ServiceException ex = assertThrows(ServiceException.class, () -> service.create(req));
        assertEquals(ErrorCodeConstants.TASK_LIST_GROUP_NAME_EMPTY.getCode(), ex.getCode());
    }

    private static TaskListGroupDO group(long id, String name, int isDefault) {
        TaskListGroupDO row = new TaskListGroupDO();
        row.setId(id);
        row.setTenantId(TENANT_ID);
        row.setListId(LIST_ID);
        row.setName(name);
        row.setRank(isDefault == 1 ? 0 : 1);
        row.setIsDefault(isDefault);
        return row;
    }
}
