package com.relayflow.module.im.service.group;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.im.controller.app.vo.AddGroupMembersReqVO;
import com.relayflow.module.im.controller.app.vo.AddGroupMembersRespVO;
import com.relayflow.module.im.controller.app.vo.CreateGroupReqVO;
import com.relayflow.module.im.controller.app.vo.CreateGroupRespVO;
import com.relayflow.module.im.controller.app.vo.GroupBotAddRespVO;
import com.relayflow.module.im.controller.app.vo.GroupBotCatalogItemRespVO;
import com.relayflow.module.im.controller.app.vo.GroupBotMembershipReqVO;
import com.relayflow.module.im.controller.app.vo.GroupBotRemoveRespVO;
import com.relayflow.module.im.controller.app.vo.GroupMemberItemRespVO;
import com.relayflow.module.im.dal.dataobject.ImBotDO;
import com.relayflow.module.im.dal.dataobject.ImBotTenantEnablementDO;
import com.relayflow.module.im.dal.dataobject.ImBotUserEnablementDO;
import com.relayflow.module.im.dal.dataobject.ImConversationDO;
import com.relayflow.module.im.dal.dataobject.ImConversationMemberDO;
import com.relayflow.module.im.dal.dataobject.ImGroupDO;
import com.relayflow.module.im.dal.mapper.ImBotMapper;
import com.relayflow.module.im.dal.mapper.ImBotTenantEnablementMapper;
import com.relayflow.module.im.dal.mapper.ImBotUserEnablementMapper;
import com.relayflow.module.im.dal.mapper.ImConversationMapper;
import com.relayflow.module.im.dal.mapper.ImConversationMemberMapper;
import com.relayflow.module.im.dal.mapper.ImGroupMapper;
import com.relayflow.module.im.enums.ErrorCodeConstants;
import com.relayflow.module.im.enums.ImBotType;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImGroupServiceImpl implements ImGroupService {

    private static final int BOT_STATUS_ENABLED = 1;
    private static final int TENANT_ENABLED = 1;

    private final ImConversationMapper conversationMapper;
    private final ImConversationMemberMapper conversationMemberMapper;
    private final ImGroupMapper groupMapper;
    private final ImBotMapper botMapper;
    private final ImBotTenantEnablementMapper tenantEnablementMapper;
    private final ImBotUserEnablementMapper userEnablementMapper;
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

        insertUserMember(tenantId, conversation.getId(), userId, ImMemberRole.OWNER, userId);
        for (Long memberUserId : memberUserIds) {
            insertUserMember(tenantId, conversation.getId(), memberUserId, ImMemberRole.MEMBER, userId);
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
            insertUserMember(tenantId, group.getConversationId(), memberUserId, ImMemberRole.MEMBER, userId);
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
                        .orderByAsc(ImConversationMemberDO::getJoinTime));

        List<Long> botIds = members.stream()
                .filter(m -> ImMemberSubjectType.BOT.equals(m.getSubjectType()))
                .map(ImConversationMemberDO::getSubjectId)
                .distinct()
                .toList();
        Map<Long, ImBotDO> botsById = loadBotsById(botIds);

        List<GroupMemberItemRespVO> items = new ArrayList<>();
        for (ImConversationMemberDO member : members) {
            GroupMemberItemRespVO item = new GroupMemberItemRespVO();
            item.setRole(member.getRole());
            if (ImMemberSubjectType.BOT.equals(member.getSubjectType())) {
                ImBotDO bot = botsById.get(member.getSubjectId());
                if (bot == null) {
                    continue;
                }
                item.setSubjectType(ImMemberSubjectType.BOT);
                item.setBotId(bot.getId());
                item.setBotCode(bot.getCode());
                item.setNickname(bot.getName());
                item.setAvatarText(firstAvatarChar(bot.getName()));
            } else {
                UserBasicDTO user = userApi.getUserBasic(member.getSubjectId());
                item.setSubjectType(ImMemberSubjectType.USER);
                item.setUserId(member.getSubjectId());
                item.setNickname(user.getNickname());
                item.setAvatarText(firstAvatarChar(user.getNickname()));
            }
            items.add(item);
        }
        return items;
    }

    @Override
    public List<GroupBotCatalogItemRespVO> listBotCatalog(Long conversationId) {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        Long tenantId = loginUser.getTenantId();
        Long userId = loginUser.getUserId();
        requireGroup(tenantId, conversationId);
        conversationService.requireMembership(tenantId, conversationId, userId);

        Set<Long> memberBotIds = loadMemberBotIds(tenantId, conversationId);
        List<ImBotDO> systemBots = botMapper.selectList(
                Wrappers.<ImBotDO>lambdaQuery()
                        .eq(ImBotDO::getStatus, BOT_STATUS_ENABLED)
                        .eq(ImBotDO::getType, ImBotType.SYSTEM)
                        .orderByAsc(ImBotDO::getId));

        List<GroupBotCatalogItemRespVO> items = new ArrayList<>();
        for (ImBotDO bot : systemBots) {
            GroupBotCatalogItemRespVO item = new GroupBotCatalogItemRespVO();
            item.setBotId(bot.getId());
            item.setBotCode(bot.getCode());
            item.setName(bot.getName());
            item.setAvatarText(firstAvatarChar(bot.getName()));
            item.setAlreadyMember(memberBotIds.contains(bot.getId()));
            items.add(item);
        }
        return items;
    }

    @Override
    @Transactional
    public GroupBotAddRespVO addBot(GroupBotMembershipReqVO request) {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        Long tenantId = loginUser.getTenantId();
        Long userId = loginUser.getUserId();
        ImGroupDO group = requireGroup(tenantId, request.getConversationId());
        requireOwner(tenantId, group.getConversationId(), userId);

        ImBotDO bot = requireActiveBot(request.getBotCode());
        requireReachable(tenantId, userId, bot);

        GroupBotAddRespVO response = new GroupBotAddRespVO();
        if (findBotMember(tenantId, group.getConversationId(), bot.getId()) != null) {
            response.setAdded(false);
            return response;
        }

        insertBotMember(tenantId, group.getConversationId(), bot.getId(), userId);
        messageService.sendSystemMessage(tenantId, group.getConversationId(),
                bot.getName() + " 加入了群聊");
        response.setAdded(true);
        return response;
    }

    @Override
    @Transactional
    public GroupBotRemoveRespVO removeBot(GroupBotMembershipReqVO request) {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        Long tenantId = loginUser.getTenantId();
        Long userId = loginUser.getUserId();
        ImGroupDO group = requireGroup(tenantId, request.getConversationId());
        requireOwner(tenantId, group.getConversationId(), userId);

        ImBotDO bot = requireBotByCode(request.getBotCode());
        ImConversationMemberDO membership = findBotMember(tenantId, group.getConversationId(), bot.getId());

        GroupBotRemoveRespVO response = new GroupBotRemoveRespVO();
        if (membership == null) {
            response.setRemoved(false);
            return response;
        }

        conversationMemberMapper.deleteById(membership.getId());
        messageService.sendSystemMessage(tenantId, group.getConversationId(),
                bot.getName() + " 离开了群聊");
        response.setRemoved(true);
        return response;
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

    private void requireOwner(Long tenantId, Long conversationId, Long userId) {
        ImConversationMemberDO membership = conversationMemberMapper.selectOne(
                Wrappers.<ImConversationMemberDO>lambdaQuery()
                        .eq(ImConversationMemberDO::getTenantId, tenantId)
                        .eq(ImConversationMemberDO::getConversationId, conversationId)
                        .eq(ImConversationMemberDO::getSubjectType, ImMemberSubjectType.USER)
                        .eq(ImConversationMemberDO::getSubjectId, userId));
        if (membership == null) {
            throw new ServiceException(ErrorCodeConstants.CONVERSATION_ACCESS_DENIED);
        }
        if (!ImMemberRole.OWNER.equals(membership.getRole())) {
            throw new ServiceException(ErrorCodeConstants.GROUP_OWNER_REQUIRED);
        }
    }

    private ImBotDO requireActiveBot(String botCode) {
        ImBotDO bot = requireBotByCode(botCode);
        if (!Objects.equals(bot.getStatus(), BOT_STATUS_ENABLED)) {
            throw new ServiceException(ErrorCodeConstants.BOT_NOT_FOUND);
        }
        return bot;
    }

    private ImBotDO requireBotByCode(String botCode) {
        if (!StringUtils.hasText(botCode)) {
            throw new ServiceException(ErrorCodeConstants.BOT_NOT_FOUND);
        }
        ImBotDO bot = botMapper.selectOne(
                Wrappers.<ImBotDO>lambdaQuery()
                        .eq(ImBotDO::getCode, botCode.trim()));
        if (bot == null) {
            throw new ServiceException(ErrorCodeConstants.BOT_NOT_FOUND);
        }
        return bot;
    }

    private void requireReachable(Long tenantId, Long userId, ImBotDO bot) {
        if (ImBotType.isSystem(bot.getType())) {
            return;
        }
        if (isTenantEnabled(tenantId, bot.getId()) || isUserEnabled(tenantId, userId, bot.getId())) {
            return;
        }
        throw new ServiceException(ErrorCodeConstants.BOT_NOT_ENABLED_FOR_TENANT);
    }

    private boolean isTenantEnabled(Long tenantId, Long botId) {
        ImBotTenantEnablementDO enablement = tenantEnablementMapper.selectOne(
                Wrappers.<ImBotTenantEnablementDO>lambdaQuery()
                        .eq(ImBotTenantEnablementDO::getTenantId, tenantId)
                        .eq(ImBotTenantEnablementDO::getBotId, botId)
                        .eq(ImBotTenantEnablementDO::getEnabled, TENANT_ENABLED));
        return enablement != null;
    }

    private boolean isUserEnabled(Long tenantId, Long userId, Long botId) {
        Long count = userEnablementMapper.selectCount(
                Wrappers.<ImBotUserEnablementDO>lambdaQuery()
                        .eq(ImBotUserEnablementDO::getTenantId, tenantId)
                        .eq(ImBotUserEnablementDO::getUserId, userId)
                        .eq(ImBotUserEnablementDO::getBotId, botId));
        return count != null && count > 0;
    }

    private ImConversationMemberDO findBotMember(Long tenantId, Long conversationId, Long botId) {
        return conversationMemberMapper.selectOne(
                Wrappers.<ImConversationMemberDO>lambdaQuery()
                        .eq(ImConversationMemberDO::getTenantId, tenantId)
                        .eq(ImConversationMemberDO::getConversationId, conversationId)
                        .eq(ImConversationMemberDO::getSubjectType, ImMemberSubjectType.BOT)
                        .eq(ImConversationMemberDO::getSubjectId, botId));
    }

    private Set<Long> loadMemberBotIds(Long tenantId, Long conversationId) {
        return conversationMemberMapper.selectList(
                        Wrappers.<ImConversationMemberDO>lambdaQuery()
                                .eq(ImConversationMemberDO::getTenantId, tenantId)
                                .eq(ImConversationMemberDO::getConversationId, conversationId)
                                .eq(ImConversationMemberDO::getSubjectType, ImMemberSubjectType.BOT))
                .stream()
                .map(ImConversationMemberDO::getSubjectId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private Map<Long, ImBotDO> loadBotsById(List<Long> botIds) {
        if (botIds.isEmpty()) {
            return Map.of();
        }
        List<ImBotDO> bots = botMapper.selectBatchIds(botIds);
        Map<Long, ImBotDO> map = new HashMap<>();
        for (ImBotDO bot : bots) {
            map.put(bot.getId(), bot);
        }
        return map;
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

    private void insertUserMember(Long tenantId, Long conversationId, Long userId, String role, Long creator) {
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

    private void insertBotMember(Long tenantId, Long conversationId, Long botId, Long creator) {
        ImConversationMemberDO member = new ImConversationMemberDO();
        member.setTenantId(tenantId);
        member.setConversationId(conversationId);
        member.setSubjectType(ImMemberSubjectType.BOT);
        member.setSubjectId(botId);
        member.setRole(ImMemberRole.MEMBER);
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
