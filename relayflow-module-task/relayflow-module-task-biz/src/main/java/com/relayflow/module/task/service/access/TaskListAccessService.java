package com.relayflow.module.task.service.access;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.task.dal.dataobject.TaskListDO;
import com.relayflow.module.task.dal.dataobject.TaskListMemberDO;
import com.relayflow.module.task.dal.mapper.TaskListMapper;
import com.relayflow.module.task.dal.mapper.TaskListMemberMapper;
import com.relayflow.module.task.enums.ErrorCodeConstants;
import com.relayflow.module.task.enums.TaskListRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TaskListAccessService {

    private final TaskListMapper taskListMapper;
    private final TaskListMemberMapper taskListMemberMapper;

    public TaskListDO requireList(Long listId) {
        TaskListDO row = taskListMapper.selectById(listId);
        if (row == null) {
            throw new ServiceException(ErrorCodeConstants.TASK_LIST_NOT_FOUND);
        }
        return row;
    }

    public TaskListMemberDO requireMembership(Long listId, Long userId) {
        TaskListMemberDO member = findMembership(listId, userId);
        if (member == null) {
            throw new ServiceException(ErrorCodeConstants.TASK_LIST_FORBIDDEN);
        }
        return member;
    }

    public TaskListMemberDO findMembership(Long listId, Long userId) {
        if (listId == null || userId == null) {
            return null;
        }
        return taskListMemberMapper.selectOne(
                Wrappers.<TaskListMemberDO>lambdaQuery()
                        .eq(TaskListMemberDO::getListId, listId)
                        .eq(TaskListMemberDO::getUserId, userId)
                        .last("LIMIT 1"));
    }

    public TaskListDO requireReadable(Long listId, Long userId) {
        TaskListDO list = requireList(listId);
        requireMembership(listId, userId);
        return list;
    }

    public TaskListDO requireOwner(Long listId, Long userId) {
        TaskListDO list = requireList(listId);
        TaskListMemberDO member = requireMembership(listId, userId);
        if (!TaskListRole.canEditMeta(member.getRole())) {
            throw new ServiceException(ErrorCodeConstants.TASK_LIST_FORBIDDEN);
        }
        return list;
    }

    public void requireCanMutateTasks(Long listId, Long userId) {
        TaskListMemberDO member = requireMembership(listId, userId);
        if (!TaskListRole.canMutateTasks(member.getRole())) {
            throw new ServiceException(ErrorCodeConstants.TASK_LIST_FORBIDDEN);
        }
    }

    public long countOwners(Long listId) {
        return taskListMemberMapper.selectCount(
                Wrappers.<TaskListMemberDO>lambdaQuery()
                        .eq(TaskListMemberDO::getListId, listId)
                        .eq(TaskListMemberDO::getRole, TaskListRole.OWNER));
    }
}
