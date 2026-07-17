package com.relayflow.module.task.service.list;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.system.api.tenant.TenantMemberApi;
import com.relayflow.module.system.api.user.UserApi;
import com.relayflow.module.system.api.user.dto.UserBasicDTO;
import com.relayflow.module.task.controller.app.vo.TaskListCreateReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListIdReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListMemberInviteReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListMemberRemoveReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListMemberRespVO;
import com.relayflow.module.task.controller.app.vo.TaskListMemberUpdateRoleReqVO;
import com.relayflow.module.task.controller.app.vo.TaskListRespVO;
import com.relayflow.module.task.controller.app.vo.TaskListUpdateReqVO;
import com.relayflow.module.task.dal.dataobject.TaskListDO;
import com.relayflow.module.task.dal.dataobject.TaskListMemberDO;
import com.relayflow.module.task.dal.mapper.TaskListMapper;
import com.relayflow.module.task.dal.mapper.TaskListMemberMapper;
import com.relayflow.module.task.enums.ErrorCodeConstants;
import com.relayflow.module.task.enums.TaskListRole;
import com.relayflow.module.task.service.access.TaskListAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TaskListServiceImpl implements TaskListService {

    private final TaskListMapper taskListMapper;
    private final TaskListMemberMapper taskListMemberMapper;
    private final TaskListAccessService taskListAccessService;
    private final TenantMemberApi tenantMemberApi;
    private final UserApi userApi;

    @Override
    public List<TaskListRespVO> listMine() {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        List<TaskListMemberDO> memberships = taskListMemberMapper.selectList(
                Wrappers.<TaskListMemberDO>lambdaQuery()
                        .eq(TaskListMemberDO::getUserId, userId));
        if (memberships == null || memberships.isEmpty()) {
            return List.of();
        }
        List<TaskListRespVO> result = new ArrayList<>();
        for (TaskListMemberDO membership : memberships) {
            TaskListDO list = taskListMapper.selectById(membership.getListId());
            if (list == null || isArchived(list)) {
                continue;
            }
            result.add(toResp(list, membership.getRole()));
        }
        result.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));
        return result;
    }

    @Override
    public TaskListRespVO get(Long id) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        TaskListDO list = taskListAccessService.requireReadable(id, userId);
        TaskListMemberDO membership = taskListAccessService.requireMembership(id, userId);
        return toResp(list, membership.getRole());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TaskListCreateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        String name = request.getName() == null ? "" : request.getName().trim();
        if (!StringUtils.hasText(name)) {
            throw new ServiceException(ErrorCodeConstants.TASK_LIST_NAME_EMPTY);
        }
        OffsetDateTime now = OffsetDateTime.now();
        TaskListDO list = new TaskListDO();
        list.setTenantId(tenantId);
        list.setName(name);
        list.setDescription(trimToNull(request.getDescription()));
        list.setOwnerId(userId);
        list.setArchived(0);
        list.setCreator(userId);
        list.setCreateTime(now);
        list.setUpdater(userId);
        list.setUpdateTime(now);
        taskListMapper.insert(list);

        TaskListMemberDO owner = new TaskListMemberDO();
        owner.setTenantId(tenantId);
        owner.setListId(list.getId());
        owner.setUserId(userId);
        owner.setRole(TaskListRole.OWNER);
        owner.setCreator(userId);
        owner.setCreateTime(now);
        owner.setUpdater(userId);
        owner.setUpdateTime(now);
        taskListMemberMapper.insert(owner);
        return list.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(TaskListUpdateReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        TaskListDO list = taskListAccessService.requireOwner(request.getId(), userId);
        if (request.getName() != null) {
            String name = request.getName().trim();
            if (!StringUtils.hasText(name)) {
                throw new ServiceException(ErrorCodeConstants.TASK_LIST_NAME_EMPTY);
            }
            list.setName(name);
        }
        if (request.getDescription() != null) {
            list.setDescription(trimToNull(request.getDescription()));
        }
        list.setUpdater(userId);
        list.setUpdateTime(OffsetDateTime.now());
        taskListMapper.updateById(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archive(TaskListIdReqVO request) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        TaskListDO list = taskListAccessService.requireOwner(request.getId(), userId);
        list.setArchived(1);
        list.setUpdater(userId);
        list.setUpdateTime(OffsetDateTime.now());
        taskListMapper.updateById(list);
    }

    @Override
    public List<TaskListMemberRespVO> listMembers(Long listId) {
        Long userId = SecurityFrameworkUtils.requireLoginUserId();
        taskListAccessService.requireReadable(listId, userId);
        List<TaskListMemberDO> members = taskListMemberMapper.selectList(
                Wrappers.<TaskListMemberDO>lambdaQuery()
                        .eq(TaskListMemberDO::getListId, listId)
                        .orderByAsc(TaskListMemberDO::getCreateTime));
        List<TaskListMemberRespVO> result = new ArrayList<>();
        for (TaskListMemberDO member : members) {
            result.add(toMemberResp(member));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inviteMember(TaskListMemberInviteReqVO request) {
        Long actorId = SecurityFrameworkUtils.requireLoginUserId();
        Long tenantId = SecurityFrameworkUtils.requireLoginTenantId();
        taskListAccessService.requireOwner(request.getListId(), actorId);
        String role = request.getRole() == null ? "" : request.getRole().trim().toUpperCase();
        if (!TaskListRole.isValidInviteRole(role)) {
            throw new ServiceException(ErrorCodeConstants.TASK_LIST_FORBIDDEN);
        }
        requireActiveMember(tenantId, request.getUserId());

        TaskListMemberDO existing = taskListAccessService.findMembership(request.getListId(), request.getUserId());
        OffsetDateTime now = OffsetDateTime.now();
        if (existing != null) {
            if (TaskListRole.isOwner(existing.getRole()) && !TaskListRole.isOwner(role)) {
                ensureRemainingOwner(request.getListId(), request.getUserId());
            }
            existing.setRole(role);
            existing.setUpdater(actorId);
            existing.setUpdateTime(now);
            taskListMemberMapper.updateById(existing);
            return;
        }

        TaskListMemberDO member = new TaskListMemberDO();
        member.setTenantId(tenantId);
        member.setListId(request.getListId());
        member.setUserId(request.getUserId());
        member.setRole(role);
        member.setCreator(actorId);
        member.setCreateTime(now);
        member.setUpdater(actorId);
        member.setUpdateTime(now);
        taskListMemberMapper.insert(member);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMemberRole(TaskListMemberUpdateRoleReqVO request) {
        Long actorId = SecurityFrameworkUtils.requireLoginUserId();
        taskListAccessService.requireOwner(request.getListId(), actorId);
        String role = request.getRole() == null ? "" : request.getRole().trim().toUpperCase();
        if (!TaskListRole.OWNER.equals(role) && !TaskListRole.isValidInviteRole(role)) {
            throw new ServiceException(ErrorCodeConstants.TASK_LIST_FORBIDDEN);
        }
        TaskListMemberDO member = taskListAccessService.requireMembership(request.getListId(), request.getUserId());
        if (TaskListRole.isOwner(member.getRole()) && !TaskListRole.isOwner(role)) {
            ensureRemainingOwner(request.getListId(), request.getUserId());
        }
        member.setRole(role);
        member.setUpdater(actorId);
        member.setUpdateTime(OffsetDateTime.now());
        taskListMemberMapper.updateById(member);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMember(TaskListMemberRemoveReqVO request) {
        Long actorId = SecurityFrameworkUtils.requireLoginUserId();
        taskListAccessService.requireOwner(request.getListId(), actorId);
        TaskListMemberDO member = taskListAccessService.requireMembership(request.getListId(), request.getUserId());
        if (TaskListRole.isOwner(member.getRole())) {
            ensureRemainingOwner(request.getListId(), request.getUserId());
        }
        taskListMemberMapper.deleteById(member.getId());
    }

    private void ensureRemainingOwner(Long listId, Long userIdBeingChanged) {
        long owners = taskListAccessService.countOwners(listId);
        TaskListMemberDO target = taskListAccessService.findMembership(listId, userIdBeingChanged);
        if (target != null && TaskListRole.isOwner(target.getRole()) && owners <= 1) {
            throw new ServiceException(ErrorCodeConstants.TASK_LIST_OWNER_REQUIRED);
        }
    }

    private void requireActiveMember(Long tenantId, Long userId) {
        Set<Long> active = tenantMemberApi.filterActiveMemberUserIds(tenantId, List.of(userId));
        if (active == null || !active.contains(userId)) {
            throw new ServiceException(ErrorCodeConstants.TASK_LIST_MEMBER_NOT_TENANT);
        }
    }

    private TaskListRespVO toResp(TaskListDO list, String myRole) {
        TaskListRespVO vo = new TaskListRespVO();
        vo.setId(list.getId());
        vo.setName(list.getName());
        vo.setDescription(list.getDescription());
        vo.setOwnerId(list.getOwnerId());
        vo.setArchived(isArchived(list));
        vo.setMyRole(myRole);
        vo.setCreateTime(list.getCreateTime());
        return vo;
    }

    private TaskListMemberRespVO toMemberResp(TaskListMemberDO member) {
        TaskListMemberRespVO vo = new TaskListMemberRespVO();
        vo.setUserId(member.getUserId());
        vo.setRole(member.getRole());
        vo.setJoinTime(member.getCreateTime());
        UserBasicDTO basic = userApi.getUserBasic(member.getUserId());
        String nickname = basic != null && StringUtils.hasText(basic.getNickname())
                ? basic.getNickname()
                : (basic != null && StringUtils.hasText(basic.getUsername()) ? basic.getUsername() : "用户");
        vo.setNickname(nickname);
        vo.setAvatarText(nickname.substring(0, 1));
        return vo;
    }

    private static boolean isArchived(TaskListDO list) {
        return list.getArchived() != null && list.getArchived() != 0;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
