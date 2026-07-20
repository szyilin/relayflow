package com.relayflow.module.task.service.listfield;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.module.task.controller.app.vo.TaskListFieldCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListFieldRespVO;
import com.relayflow.module.task.dal.dataobject.TaskListFieldDO;
import com.relayflow.module.task.dal.dataobject.TaskListFieldOptionDO;
import com.relayflow.module.task.dal.mapper.TaskItemFieldValueMapper;
import com.relayflow.module.task.dal.mapper.TaskListFieldMapper;
import com.relayflow.module.task.dal.mapper.TaskListFieldOptionMapper;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskListFieldServiceImplTest {

    private static final long USER_ID = 100L;
    private static final long TENANT_ID = 1L;
    private static final long LIST_ID = 501L;

    @Mock
    private TaskListFieldMapper taskListFieldMapper;
    @Mock
    private TaskListFieldOptionMapper taskListFieldOptionMapper;
    @Mock
    private TaskItemFieldValueMapper taskItemFieldValueMapper;
    @Mock
    private TaskListItemMapper taskListItemMapper;
    @Mock
    private TaskListAccessService taskListAccessService;
    @Mock
    private TaskAccessService taskAccessService;

    private TaskListFieldServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TaskListFieldServiceImpl(
                taskListFieldMapper,
                taskListFieldOptionMapper,
                taskItemFieldValueMapper,
                taskListItemMapper,
                taskListAccessService,
                taskAccessService);
        LoginUser loginUser = new LoginUser(USER_ID, "u", TENANT_ID, "member", List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void create_persistsFieldAndOptions() {
        when(taskListFieldMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        doAnswer(invocation -> {
            TaskListFieldDO row = invocation.getArgument(0);
            row.setId(9001L);
            return 1;
        }).when(taskListFieldMapper).insert(any(TaskListFieldDO.class));
        doAnswer(invocation -> {
            TaskListFieldOptionDO row = invocation.getArgument(0);
            if (row.getId() == null) {
                row.setId(row.getRank() == null ? 1L : 10L + row.getRank());
            }
            return 1;
        }).when(taskListFieldOptionMapper).insert(any(TaskListFieldOptionDO.class));

        TaskListFieldCreateReqVO req = new TaskListFieldCreateReqVO();
        req.setListId(LIST_ID);
        req.setName("优先级");
        TaskListFieldCreateReqVO.OptionDraft a = new TaskListFieldCreateReqVO.OptionDraft();
        a.setLabel("高");
        a.setValueKey("high");
        TaskListFieldCreateReqVO.OptionDraft b = new TaskListFieldCreateReqVO.OptionDraft();
        b.setLabel("低");
        req.setOptions(List.of(a, b));

        TaskListFieldRespVO resp = service.create(req);

        assertEquals(9001L, resp.getId());
        assertEquals("custom:9001", resp.getFieldKey());
        assertEquals(2, resp.getOptions().size());
        assertEquals("high", resp.getOptions().get(0).getValueKey());
        verify(taskListAccessService).requireCanMutateTasks(LIST_ID, USER_ID);

        ArgumentCaptor<TaskListFieldDO> fieldCaptor = ArgumentCaptor.forClass(TaskListFieldDO.class);
        verify(taskListFieldMapper).updateById(fieldCaptor.capture());
        assertEquals("custom:9001", fieldCaptor.getValue().getFieldKey());
    }

    @Test
    void create_rejectsFewerThanTwoOptions() {
        TaskListFieldCreateReqVO req = new TaskListFieldCreateReqVO();
        req.setListId(LIST_ID);
        req.setName("X");
        TaskListFieldCreateReqVO.OptionDraft a = new TaskListFieldCreateReqVO.OptionDraft();
        a.setLabel("仅一个");
        req.setOptions(List.of(a));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.create(req));
        assertEquals(ErrorCodeConstants.TASK_LIST_FIELD_OPTIONS_MIN.getCode(), ex.getCode());
    }

    @Test
    void parseCustomFieldId_ok() {
        assertEquals(2002L, TaskListFieldServiceImpl.parseCustomFieldId("custom:2002"));
        assertTrue(TaskListFieldServiceImpl.isCustomFieldKey("custom:1"));
    }
}
