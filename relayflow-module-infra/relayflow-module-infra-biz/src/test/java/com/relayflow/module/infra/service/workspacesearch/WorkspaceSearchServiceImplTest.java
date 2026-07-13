package com.relayflow.module.infra.service.workspacesearch;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.im.api.conversation.ImConversationApi;
import com.relayflow.module.im.api.conversation.dto.ConversationSearchRespDTO;
import com.relayflow.module.infra.enums.ErrorCodeConstants;
import com.relayflow.module.system.api.user.MemberUserApi;
import com.relayflow.module.system.api.user.dto.MemberSearchRespDTO;
import com.relayflow.module.task.api.item.TaskItemApi;
import com.relayflow.module.task.api.item.dto.TaskSearchRespDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceSearchServiceImplTest {

    private static final long TENANT_ID = 1L;
    private static final long USER_ID = 100L;

    @Mock
    private MemberUserApi memberUserApi;
    @Mock
    private ImConversationApi imConversationApi;
    @Mock
    private TaskItemApi taskItemApi;

    @InjectMocks
    private WorkspaceSearchServiceImpl workspaceSearchService;

    @Test
    void search_rejectsBlankKeyword() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> workspaceSearchService.search(TENANT_ID, USER_ID, "   ", 5));
        assertEquals(ErrorCodeConstants.SEARCH_KEYWORD_REQUIRED.getCode(), ex.getCode());
    }

    @Test
    void search_returnsGroupedResults() {
        MemberSearchRespDTO member = new MemberSearchRespDTO();
        member.setUserId(1001L);
        member.setNickname("张三");
        member.setDeptName("研发部");

        ConversationSearchRespDTO conversation = new ConversationSearchRespDTO();
        conversation.setConversationId(2001L);
        conversation.setTitle("项目群");
        conversation.setSubtitle("你好");

        TaskSearchRespDTO task = new TaskSearchRespDTO();
        task.setTaskId(3001L);
        task.setTitle("周报");
        task.setStatus("TODO");

        when(memberUserApi.searchMembers(eq(TENANT_ID), eq("张"), eq(5))).thenReturn(List.of(member));
        when(imConversationApi.searchConversations(eq(TENANT_ID), eq(USER_ID), eq("张"), eq(5)))
                .thenReturn(List.of(conversation));
        when(taskItemApi.searchTasks(eq(TENANT_ID), eq(USER_ID), eq("张"), eq(5))).thenReturn(List.of(task));

        var result = workspaceSearchService.search(TENANT_ID, USER_ID, "张", 5);

        assertEquals("张", result.getKeyword());
        assertEquals(3, result.getGroups().size());
        assertEquals("member", result.getGroups().get(0).getType());
        assertEquals("/app/contacts?memberId=1001", result.getGroups().get(0).getItems().get(0).getRoute());
        verify(memberUserApi).searchMembers(TENANT_ID, "张", 5);
    }
}
