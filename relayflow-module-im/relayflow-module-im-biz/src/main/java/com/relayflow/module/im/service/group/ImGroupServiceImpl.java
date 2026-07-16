package com.relayflow.module.im.service.group;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.im.controller.app.vo.AddGroupMembersReqVO;
import com.relayflow.module.im.controller.app.vo.AddGroupMembersRespVO;
import com.relayflow.module.im.controller.app.vo.CreateGroupReqVO;
import com.relayflow.module.im.controller.app.vo.CreateGroupRespVO;
import com.relayflow.module.im.controller.app.vo.GroupMemberItemRespVO;
import com.relayflow.module.im.dal.dataobject.ImConversationDO;
import com.relayflow.module.im.dal.dataobject.ImConversationMemberDO;
import com.relayflow.module.im.dal.dataobject.ImGroupDO;
import com.relayflow.module.im.dal.mapper.ImConversationMapper;
import com.relayflow.module.im.dal.mapper.ImConversationMemberMapper;
import com.relayflow.module.im.dal.mapper.ImGroupMapper;
import com.relayflow.module.im.enums.ErrorCodeConstants;
import com.relayflow.module.im.enums.ImConversationType;
import com.relayflow.module.im.enums.ImMemberRole;
import com.relayflow.module.im.enums.ImMemberSubjectType;
import com.relayflow.module.im.service.conversation.ImConversationService;
import com.relayflow.module.im.service.message.ImMessageService;
import com.relayflow.module.system.api.user.UserApi;
import com.relayflow.module.system.api.user.dto.UserBasicDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImGroupServiceImpl implements ImGroupService {

    private final ImConversationMapper conversationMapper;
    private final ImConversationMemberMapper conversationMemberMapper;
    private final ImGroupMapper groupMapper;
    private final ImConversationService conversationService;
    private final ImMessageService messageService;
    private final UserApi userApi;

    @Override
    @Transactional
    public CreateGroupRespVO createGroup(CreateGroupReqVO request) {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        return createGroup(loginUser.getTenantId(), loginUser.getUserId(), request);
    }

    private CreateGroupRespVO createGroup(Long tenantId, Long userId, CreateGroupReqVO request) {
        String name = normalizeGroupName(request.getName());
        List<Long> memberUserIds = normalizeMemberUserIds(userId, request.getMemberUserIds());
        validateMembersExist(memberUserIds);

        ImConversationDO conversation = new ImConversationDO();
        conversation.setTenantId(tenantId);
        conversation.setType(ImConversationType.GROUP);
        conversation.setTitle(name);
        conversation.setCreator(userId);
        conversationMapper.insert(conversation);

        ImGroupDO group = new ImGroupDO();
        group.setTenantId(tenantId);
        group.setConversationId(conversation.getId());
        group.setName(name);
        group.setOwnerUserId(userId);
        group.setCreator(userId);
        groupMapper.insert(group);

        insertMember(tenantId, conversation.getId(), userId, ImMemberRole.OWNER, userId);
        for (Long memberUserId : memberUserIds) {
            insertMember(tenantId, conversation.getId(), memberUserId, ImMemberRole.MEMBER, userId);
            UserBasicDTO member = userApi.getUserBasic(memberUserId);
            messageService.sendSystemMessage(tenantId, conversation.getId(),
                    member.getNickname() + " 加入了群聊");
        }

        CreateGroupRespVO response = new CreateGroupRespVO();
        response.setConversationId(conversation.getId());
        response.setGroupId(group.getId());
        return response;
    }

    @Override
    @Transactional
    public AddGroupMembersRespVO addMembers(AddGroupMembersReqVO request) {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        return addMembers(loginUser.getTenantId(), loginUser.getUserId(), request);
    }

    private AddGroupMembersRespVO addMembers(Long tenantId, Long userId, AddGroupMembersReqVO request) {
        ImGroupDO group = requireGroup(tenantId, request.getConversationId());
        conversationService.requireMembership(tenantId, group.getConversationId(), userId);

        List<Long> memberUserIds = normalizeMemberUserIds(userId, request.getMemberUserIds());
        if (memberUserIds.isEmpty()) {
            AddGroupMembersRespVO empty = new AddGroupMembersRespVO();
            empty.setAddedCount(0);
            return empty;
        }

        validateMembersExist(memberUserIds);

        Set<Long> existingMemberIds = loadMemberUserIds(tenantId, group.getConversationId());
        int addedCount = 0;
        for (Long memberUserId : memberUserIds) {
            if (existingMemberIds.contains(memberUserId)) {
                continue;
            }
            insertMember(tenantId, group.getConversationId(), memberUserId, ImMemberRole.MEMBER, userId);
            UserBasicDTO member = userApi.getUserBasic(memberUserId);
            messageService.sendSystemMessage(tenantId, group.getConversationId(),
                    member.getNickname() + " 加入了群聊");
            addedCount++;
        }

        AddGroupMembersRespVO response = new AddGroupMembersRespVO();
        response.setAddedCount(addedCount);
        return response;
    }

    @Override
    public List<GroupMemberItemRespVO> listMembers(Long conversationId) {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        return listMembers(loginUser.getTenantId(), loginUser.getUserId(), conversationId);
    }

    private List<GroupMemberItemRespVO> listMembers(Long tenantId, Long userId, Long conversationId) {
        requireGroup(tenantId, conversationId);
        conversationService.requireMembership(tenantId, conversationId, userId);

        List<ImConversationMemberDO> members = conversationMemberMapper.selectList(
                Wrappers.<ImConversationMemberDO>lambdaQuery()
                        .eq(ImConversationMemberDO::getTenantId, tenantId)
                        .eq(ImConversationMemberDO::getConversationId, conversationId)
                        .eq(ImConversationMemberDO::getSubjectType, ImMemberSubjectType.USER)
                        .orderByAsc(ImConversationMemberDO::getJoinTime));

        List<GroupMemberItemRespVO> items = new ArrayList<>();
        for (ImConversationMemberDO member : members) {
            UserBasicDTO user = userApi.getUserBasic(member.getSubjectId());
            GroupMemberItemRespVO item = new GroupMemberItemRespVO();
            item.setUserId(member.getSubjectId());
            item.setNickname(user.getNickname());
            item.setAvatarText(firstAvatarChar(user.getNickname()));
            item.setRole(member.getRole());
            items.add(item);
        }
        return items;
    }

    public ImGroupDO requireGroup(Long tenantId, Long conversationId) {
        ImConversationDO conversation = conversationService.requireConversation(tenantId, conversationId);
        if (!ImConversationType.GROUP.equals(conversation.getType())) {
            throw new ServiceException(ErrorCodeConstants.GROUP_NOT_FOUND);
        }
        ImGroupDO group = groupMapper.selectOne(
                Wrappers.<ImGroupDO>lambdaQuery()
                        .eq(ImGroupDO::getTenantId, tenantId)
                        .eq(ImGroupDO::getConversationId, conversationId));
        if (group == null) {
            throw new ServiceException(ErrorCodeConstants.GROUP_NOT_FOUND);
        }
        return group;
    }

    private String normalizeGroupName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new ServiceException(ErrorCodeConstants.GROUP_NAME_INVALID);
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.length() > 128) {
            throw new ServiceException(ErrorCodeConstants.GROUP_NAME_INVALID);
        }
        return trimmed;
    }

    private List<Long> normalizeMemberUserIds(Long operatorUserId, List<Long> memberUserIds) {
        if (memberUserIds == null || memberUserIds.isEmpty()) {
            throw new ServiceException(ErrorCodeConstants.GROUP_MEMBER_REQUIRED);
        }
        Set<Long> unique = new LinkedHashSet<>();
        for (Long memberUserId : memberUserIds) {
            if (memberUserId == null || Objects.equals(memberUserId, operatorUserId)) {
                continue;
            }
            unique.add(memberUserId);
        }
        if (unique.isEmpty()) {
            throw new ServiceException(ErrorCodeConstants.GROUP_MEMBER_REQUIRED);
        }
        return List.copyOf(unique);
    }

    private void validateMembersExist(List<Long> memberUserIds) {
        for (Long memberUserId : memberUserIds) {
            try {
                userApi.getUserBasic(memberUserId);
            } catch (RuntimeException ex) {
                throw new ServiceException(ErrorCodeConstants.GROUP_MEMBER_INVALID);
            }
        }
    }

    private Set<Long> loadMemberUserIds(Long tenantId, Long conversationId) {
        return new LinkedHashSet<>(conversationMemberMapper.selectList(
                        Wrappers.<ImConversationMemberDO>lambdaQuery()
                                .eq(ImConversationMemberDO::getTenantId, tenantId)
                                .eq(ImConversationMemberDO::getConversationId, conversationId)
                                .eq(ImConversationMemberDO::getSubjectType, ImMemberSubjectType.USER))
                .stream()
                .map(ImConversationMemberDO::getSubjectId)
                .toList());
    }

    private void insertMember(Long tenantId, Long conversationId, Long userId, String role, Long creator) {
        ImConversationMemberDO member = new ImConversationMemberDO();
        member.setTenantId(tenantId);
        member.setConversationId(conversationId);
        member.setSubjectType(ImMemberSubjectType.USER);
        member.setSubjectId(userId);
        member.setRole(role);
        member.setReadSeq(0L);
        member.setUnreadCount(0);
        member.setJoinTime(OffsetDateTime.now());
        member.setPinned(0);
        member.setCreator(creator);
        conversationMemberMapper.insert(member);
    }

    private String firstAvatarChar(String displayName) {
        if (!StringUtils.hasText(displayName)) {
            return "?";
        }
        return displayName.substring(0, 1);
    }
}
