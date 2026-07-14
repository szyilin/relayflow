package com.relayflow.module.infra.service.workspacesearch;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.im.api.conversation.ImConversationApi;
import com.relayflow.module.im.api.conversation.dto.ConversationSearchRespDTO;
import com.relayflow.module.infra.controller.app.workspacesearch.vo.WorkspaceSearchGroupRespVO;
import com.relayflow.module.infra.controller.app.workspacesearch.vo.WorkspaceSearchItemRespVO;
import com.relayflow.module.infra.controller.app.workspacesearch.vo.WorkspaceSearchRespVO;
import com.relayflow.module.infra.enums.ErrorCodeConstants;
import com.relayflow.module.system.api.user.MemberUserApi;
import com.relayflow.module.system.api.user.dto.MemberSearchRespDTO;
import com.relayflow.module.task.api.item.TaskItemApi;
import com.relayflow.module.task.api.item.dto.TaskSearchRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class WorkspaceSearchServiceImpl implements WorkspaceSearchService {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 10;

    private final MemberUserApi memberUserApi;
    private final ImConversationApi imConversationApi;
    private final TaskItemApi taskItemApi;

    @Override
    public WorkspaceSearchRespVO search(String keyword, int limitPerGroup) {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        Long tenantId = loginUser.getTenantId();
        Long userId = loginUser.getUserId();

        String trimmed = requireKeyword(keyword);
        int limit = clampLimit(limitPerGroup);

        CompletableFuture<List<MemberSearchRespDTO>> membersFuture = CompletableFuture.supplyAsync(
                () -> memberUserApi.searchMembers(tenantId, trimmed, limit));
        CompletableFuture<List<ConversationSearchRespDTO>> conversationsFuture = CompletableFuture.supplyAsync(
                () -> imConversationApi.searchConversations(tenantId, userId, trimmed, limit));
        CompletableFuture<List<TaskSearchRespDTO>> tasksFuture = CompletableFuture.supplyAsync(
                () -> taskItemApi.searchTasks(tenantId, userId, trimmed, limit));

        CompletableFuture.allOf(membersFuture, conversationsFuture, tasksFuture).join();

        WorkspaceSearchRespVO response = new WorkspaceSearchRespVO();
        response.setKeyword(trimmed);
        response.setGroups(List.of(
                buildMemberGroup(membersFuture.join()),
                buildConversationGroup(conversationsFuture.join()),
                buildTaskGroup(tasksFuture.join())));
        return response;
    }

    private WorkspaceSearchGroupRespVO buildMemberGroup(List<MemberSearchRespDTO> members) {
        WorkspaceSearchGroupRespVO group = new WorkspaceSearchGroupRespVO();
        group.setType("member");
        group.setLabel("联系人");
        group.setItems(members.stream().map(this::toMemberItem).toList());
        return group;
    }

    private WorkspaceSearchGroupRespVO buildConversationGroup(List<ConversationSearchRespDTO> conversations) {
        WorkspaceSearchGroupRespVO group = new WorkspaceSearchGroupRespVO();
        group.setType("conversation");
        group.setLabel("消息");
        group.setItems(conversations.stream().map(this::toConversationItem).toList());
        return group;
    }

    private WorkspaceSearchGroupRespVO buildTaskGroup(List<TaskSearchRespDTO> tasks) {
        WorkspaceSearchGroupRespVO group = new WorkspaceSearchGroupRespVO();
        group.setType("task");
        group.setLabel("任务");
        group.setItems(tasks.stream().map(this::toTaskItem).toList());
        return group;
    }

    private WorkspaceSearchItemRespVO toMemberItem(MemberSearchRespDTO member) {
        WorkspaceSearchItemRespVO item = new WorkspaceSearchItemRespVO();
        item.setId(String.valueOf(member.getUserId()));
        item.setTitle(member.getNickname());
        item.setSubtitle(member.getDeptName());
        item.setRoute("/app/contacts?memberId=" + member.getUserId());
        item.setEntityType("member");
        item.setEntityId(String.valueOf(member.getUserId()));
        return item;
    }

    private WorkspaceSearchItemRespVO toConversationItem(ConversationSearchRespDTO conversation) {
        WorkspaceSearchItemRespVO item = new WorkspaceSearchItemRespVO();
        item.setId(String.valueOf(conversation.getConversationId()));
        item.setTitle(conversation.getTitle());
        item.setSubtitle(conversation.getSubtitle());
        item.setRoute("/app/messages?conversationId=" + conversation.getConversationId());
        item.setEntityType("conversation");
        item.setEntityId(String.valueOf(conversation.getConversationId()));
        return item;
    }

    private WorkspaceSearchItemRespVO toTaskItem(TaskSearchRespDTO task) {
        WorkspaceSearchItemRespVO item = new WorkspaceSearchItemRespVO();
        item.setId(String.valueOf(task.getTaskId()));
        item.setTitle(task.getTitle());
        item.setSubtitle(task.getStatus());
        item.setRoute("/app/tasks?taskId=" + task.getTaskId());
        item.setEntityType("task");
        item.setEntityId(String.valueOf(task.getTaskId()));
        return item;
    }

    private String requireKeyword(String keyword) {
        if (!StringUtils.hasText(keyword) || !StringUtils.hasText(keyword.trim())) {
            throw new ServiceException(ErrorCodeConstants.SEARCH_KEYWORD_REQUIRED);
        }
        String trimmed = keyword.trim();
        if (trimmed.length() > 50) {
            trimmed = trimmed.substring(0, 50);
        }
        return trimmed;
    }

    private static int clampLimit(int limitPerGroup) {
        if (limitPerGroup <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limitPerGroup, MAX_LIMIT);
    }
}
