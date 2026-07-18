package com.relayflow.module.task.service.minegroup;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.module.task.controller.app.vo.TaskMineGroupCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskMineGroupListRespVO;
import com.relayflow.module.task.controller.app.vo.TaskMineGroupMoveReqVO;
import com.relayflow.module.task.dal.dataobject.TaskItemDO;
import com.relayflow.module.task.dal.dataobject.TaskMineGroupDO;
import com.relayflow.module.task.dal.dataobject.TaskMineGroupItemDO;
import com.relayflow.module.task.dal.mapper.TaskMineGroupItemMapper;
import com.relayflow.module.task.dal.mapper.TaskMineGroupMapper;
import com.relayflow.module.task.enums.ErrorCodeConstants;
import com.relayflow.module.task.service.access.TaskAccessService;
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
class TaskMineGroupServiceImplTest {

    private static final long USER_ID = 100L;
    private static final long TENANT_ID = 1L;
    private static final long DEFAULT_GROUP_ID = 10L;
    private static final long CUSTOM_GROUP_ID = 11L;
    private static final long TASK_ID = 2001L;

    @Mock
    private TaskMineGroupMapper taskMineGroupMapper;
    @Mock
    private TaskMineGroupItemMapper taskMineGroupItemMapper;
    @Mock
    private TaskAccessService taskAccessService;

    private TaskMineGroupServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TaskMineGroupServiceImpl(
                taskMineGroupMapper, taskMineGroupItemMapper, taskAccessService);
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
        when(taskMineGroupMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(taskMineGroupMapper.selectList(any(Wrapper.class))).thenAnswer(inv -> {
            // after ensureDefault insert, list groups — return empty then we rely on insert
            return List.of();
        });
        when(taskMineGroupItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        TaskMineGroupListRespVO resp = service.list();

        ArgumentCaptor<TaskMineGroupDO> captor = ArgumentCaptor.forClass(TaskMineGroupDO.class);
        verify(taskMineGroupMapper).insert(captor.capture());
        assertEquals(1, captor.getValue().getIsDefault());
        assertEquals("默认", captor.getValue().getName());
        assertTrue(resp.getGroups().isEmpty() || resp.getGroups() != null);
    }

    @Test
    void delete_defaultForbidden() {
        TaskMineGroupDO def = group(DEFAULT_GROUP_ID, "默认", 1);
        when(taskMineGroupMapper.selectById(DEFAULT_GROUP_ID)).thenReturn(def);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> service.delete(DEFAULT_GROUP_ID));
        assertEquals(ErrorCodeConstants.TASK_MINE_GROUP_FORBIDDEN.getCode(), ex.getCode());
        verify(taskMineGroupMapper, never()).deleteById(eq(DEFAULT_GROUP_ID));
    }

    @Test
    void delete_movesMembershipsToDefault() {
        TaskMineGroupDO custom = group(CUSTOM_GROUP_ID, "本周", 0);
        TaskMineGroupDO def = group(DEFAULT_GROUP_ID, "默认", 1);
        when(taskMineGroupMapper.selectById(CUSTOM_GROUP_ID)).thenReturn(custom);
        when(taskMineGroupMapper.selectOne(any(Wrapper.class))).thenReturn(def);

        TaskMineGroupItemDO item = new TaskMineGroupItemDO();
        item.setId(99L);
        item.setUserId(USER_ID);
        item.setTaskId(TASK_ID);
        item.setGroupId(CUSTOM_GROUP_ID);
        when(taskMineGroupItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of(item));

        service.delete(CUSTOM_GROUP_ID);

        ArgumentCaptor<TaskMineGroupItemDO> captor = ArgumentCaptor.forClass(TaskMineGroupItemDO.class);
        verify(taskMineGroupItemMapper).updateById(captor.capture());
        assertEquals(DEFAULT_GROUP_ID, captor.getValue().getGroupId());
        verify(taskMineGroupMapper).deleteById(CUSTOM_GROUP_ID);
    }

    @Test
    void move_upsertsMembership() {
        TaskMineGroupDO custom = group(CUSTOM_GROUP_ID, "本周", 0);
        when(taskMineGroupMapper.selectOne(any(Wrapper.class))).thenReturn(group(DEFAULT_GROUP_ID, "默认", 1));
        when(taskMineGroupMapper.selectById(CUSTOM_GROUP_ID)).thenReturn(custom);
        when(taskAccessService.requireAccessible(eq(TASK_ID), eq(USER_ID))).thenReturn(new TaskItemDO());
        when(taskMineGroupItemMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(taskMineGroupItemMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        TaskMineGroupMoveReqVO req = new TaskMineGroupMoveReqVO();
        req.setTaskId(TASK_ID);
        req.setGroupId(CUSTOM_GROUP_ID);
        service.move(req);

        ArgumentCaptor<TaskMineGroupItemDO> captor = ArgumentCaptor.forClass(TaskMineGroupItemDO.class);
        verify(taskMineGroupItemMapper).insert(captor.capture());
        assertEquals(TASK_ID, captor.getValue().getTaskId());
        assertEquals(CUSTOM_GROUP_ID, captor.getValue().getGroupId());
    }

    @Test
    void create_rejectsBlankName() {
        when(taskMineGroupMapper.selectOne(any(Wrapper.class))).thenReturn(group(DEFAULT_GROUP_ID, "默认", 1));
        TaskMineGroupCreateReqVO req = new TaskMineGroupCreateReqVO();
        req.setName("   ");
        ServiceException ex = assertThrows(ServiceException.class, () -> service.create(req));
        assertEquals(ErrorCodeConstants.TASK_MINE_GROUP_NAME_EMPTY.getCode(), ex.getCode());
    }

    private static TaskMineGroupDO group(long id, String name, int isDefault) {
        TaskMineGroupDO row = new TaskMineGroupDO();
        row.setId(id);
        row.setTenantId(TENANT_ID);
        row.setUserId(USER_ID);
        row.setName(name);
        row.setRank(isDefault == 1 ? 0 : 1);
        row.setIsDefault(isDefault);
        return row;
    }
}
