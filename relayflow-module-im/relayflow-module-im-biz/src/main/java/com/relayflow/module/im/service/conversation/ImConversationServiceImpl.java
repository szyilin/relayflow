package com.relayflow.module.im.service.conversation;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.im.service.conversation.model.ConversationListItem;
import com.relayflow.module.im.controller.app.vo.ConversationMemberReadStatusRespVO;
import com.relayflow.module.im.controller.app.vo.ConversationReadStatusRespVO;
import com.relayflow.module.im.dal.dataobject.ImBotDO;
import com.relayflow.module.im.dal.dataobject.ImConversationDO;
import com.relayflow.module.im.dal.dataobject.ImConversationMemberDO;
import com.relayflow.module.im.dal.dataobject.ImGroupDO;
import com.relayflow.module.im.dal.dataobject.ImMessageDO;
import com.relayflow.module.im.dal.mapper.ImBotMapper;
import com.relayflow.module.im.dal.mapper.ImConversationMapper;
import com.relayflow.module.im.dal.mapper.ImConversationMemberMapper;
import com.relayflow.module.im.dal.mapper.ImGroupMapper;
import com.relayflow.module.im.dal.mapper.ImMessageMapper;
import com.relayflow.module.im.enums.ErrorCodeConstants;
import com.relayflow.module.im.enums.ImConversationType;
import com.relayflow.module.im.enums.ImMemberSubjectType;
import com.relayflow.module.im.enums.ImRealtimeTypes;
import com.relayflow.module.im.service.message.ImContentHelper;
import com.relayflow.module.infra.api.realtime.RealtimeTransportApi;
import com.relayflow.module.infra.api.realtime.dto.RealtimeEnvelopeDTO;
import com.relayflow.module.system.api.user.UserApi;
import com.relayflow.module.system.api.user.dto.UserBasicDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImConversationServiceImpl implements ImConversationService {

    private final ImConversationMapper conversationMapper;
    private final ImConversationMemberMapper conversationMemberMapper;
    private final ImMessageMapper messageMapper;
    private final ImGroupMapper groupMapper;
    private final ImBotMapper botMapper;
    private final UserApi userApi;
    private final ImContentHelper contentHelper;
    private final RealtimeTransportApi realtimeTransportApi;

    @Override
    public List<ConversationListItem> listConversations(Long tenantId, Long userId, String keyword) {
        List<ImConversationMemberDO> memberships = conversationMemberMapper.selectList(
                Wrappers.<ImConversationMemberDO>lambdaQuery()
                        .eq(ImConversationMemberDO::getTenantId, tenantId)
                        .eq(ImConversationMemberDO::getSubjectType, ImMemberSubjectType.USER)
                        .eq(ImConversationMemberDO::getSubjectId, userId));
        if (memberships.isEmpty()) {
            return List.of();
        }

        Map<Long, ImConversationMemberDO> membershipByConversationId = memberships.stream()
                .collect(Collectors.toMap(ImConversationMemberDO::getConversationId, member -> member, (left, right) -> left));

        List<ImConversationDO> conversations = conversationMapper.selectList(
                Wrappers.<ImConversationDO>lambdaQuery()
                        .eq(ImConversationDO::getTenantId, tenantId)
                        .in(ImConversationDO::getId, membershipByConversationId.keySet())
                        .in(ImConversationDO::getType,
                                ImConversationType.DIRECT, ImConversationType.GROUP, ImConversationType.BOT_DM));

        List<ImConversationDO> directConversations = conversations.stream()
                .filter(conversation -> ImConversationType.DIRECT.equals(conversation.getType()))
                .toList();
        List<ImConversationDO> groupConversations = conversations.stream()
                .filter(conversation -> ImConversationType.GROUP.equals(conversation.getType()))
                .toList();
        List<ImConversationDO> botDmConversations = conversations.stream()
                .filter(conversation -> ImConversationType.BOT_DM.equals(conversation.getType()))
                .toList();

        Map<Long, Long> peerUserIdByConversationId = loadPeerUserIds(tenantId, userId, directConversations);
        Map<Long, ImGroupDO> groupByConversationId = loadGroups(tenantId, groupConversations);
        Map<Long, Integer> memberCountByConversationId = loadMemberCounts(tenantId, groupConversations);
        Map<Long, ImBotDO> botById = loadBots(botDmConversations);

        List<ConversationListItem> items = new ArrayList<>();
        for (ImConversationDO conversation : directConversations) {
            Long peerUserId = peerUserIdByConversationId.get(conversation.getId());
            if (peerUserId == null) {
                continue;
            }
            UserBasicDTO peer = userApi.getUserBasic(peerUserId);
            String title = peer.getNickname();
            ImConversationMemberDO membership = membershipByConversationId.get(conversation.getId());

            ConversationListItem item = new ConversationListItem();
            item.setId(conversation.getId());
            item.setType(conversation.getType());
            item.setTitle(title);
            item.setAvatarText(contentHelper.firstAvatarChar(title));
            item.setLastMsgPreview(conversation.getLastMsgPreview());
            item.setLastMsgAt(conversation.getLastMsgAt());
            item.setUnreadCount(membership != null && membership.getUnreadCount() != null ? membership.getUnreadCount() : 0);
            item.setPeerUserId(peerUserId);
            items.add(item);
        }

        for (ImConversationDO conversation : groupConversations) {
            ImGroupDO group = groupByConversationId.get(conversation.getId());
            if (group == null) {
                continue;
            }
            ImConversationMemberDO membership = membershipByConversationId.get(conversation.getId());
            String title = group.getName();

            ConversationListItem item = new ConversationListItem();
            item.setId(conversation.getId());
            item.setType(conversation.getType());
            item.setTitle(title);
            item.setAvatarText(contentHelper.firstAvatarChar(title));
            item.setLastMsgPreview(conversation.getLastMsgPreview());
            item.setLastMsgAt(conversation.getLastMsgAt());
            item.setUnreadCount(membership != null && membership.getUnreadCount() != null ? membership.getUnreadCount() : 0);
            item.setMemberCount(memberCountByConversationId.getOrDefault(conversation.getId(), 0));
            items.add(item);
        }

        for (ImConversationDO conversation : botDmConversations) {
            ImBotDO bot = conversation.getBotPeerBotId() != null
                    ? botById.get(conversation.getBotPeerBotId())
                    : null;
            if (bot == null) {
                continue;
            }
            ImConversationMemberDO membership = membershipByConversationId.get(conversation.getId());
            String title = bot.getName();

            ConversationListItem item = new ConversationListItem();
            item.setId(conversation.getId());
            item.setType(conversation.getType());
            item.setTitle(title);
            item.setAvatarText(contentHelper.firstAvatarChar(title));
            item.setLastMsgPreview(conversation.getLastMsgPreview());
            item.setLastMsgAt(conversation.getLastMsgAt());
            item.setUnreadCount(membership != null && membership.getUnreadCount() != null ? membership.getUnreadCount() : 0);
            item.setBotId(bot.getId());
            item.setBotCode(bot.getCode());
            items.add(item);
        }

        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;
        return items.stream()
                .filter(item -> matchesKeyword(item, normalizedKeyword))
                .sorted(Comparator.comparing(ConversationListItem::getLastMsgAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Override
    public List<ConversationListItem> listMyConversations(String keyword) {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        return listConversations(loginUser.getTenantId(), loginUser.getUserId(), keyword);
    }

    @Override
    @Transactional
    public Long getOrCreateDirectConversation(Long tenantId, Long userId, Long peerUserId) {
        if (peerUserId == null) {
            throw new ServiceException(ErrorCodeConstants.PEER_USER_INVALID);
        }
        boolean selfChat = Objects.equals(userId, peerUserId);
        // Validate peer exists (self or other tenant member lookup via userApi).
        userApi.getUserBasic(peerUserId);

        long low = Math.min(userId, peerUserId);
        long high = Math.max(userId, peerUserId);

        ImConversationDO existing = conversationMapper.selectOne(
                Wrappers.<ImConversationDO>lambdaQuery()
                        .eq(ImConversationDO::getTenantId, tenantId)
                        .eq(ImConversationDO::getType, ImConversationType.DIRECT)
                        .eq(ImConversationDO::getDirectPeerLow, low)
                        .eq(ImConversationDO::getDirectPeerHigh, high));
        if (existing != null) {
            ensureMembership(tenantId, existing.getId(), userId);
            if (!selfChat) {
                ensureMembership(tenantId, existing.getId(), peerUserId);
            }
            return existing.getId();
        }

        ImConversationDO conversation = new ImConversationDO();
        conversation.setTenantId(tenantId);
        conversation.setType(ImConversationType.DIRECT);
        conversation.setDirectPeerLow(low);
        conversation.setDirectPeerHigh(high);
        conversation.setCreator(userId);
        try {
            conversationMapper.insert(conversation);
        } catch (DuplicateKeyException ex) {
            ImConversationDO raced = conversationMapper.selectOne(
                    Wrappers.<ImConversationDO>lambdaQuery()
                            .eq(ImConversationDO::getTenantId, tenantId)
                            .eq(ImConversationDO::getType, ImConversationType.DIRECT)
                            .eq(ImConversationDO::getDirectPeerLow, low)
                            .eq(ImConversationDO::getDirectPeerHigh, high));
            if (raced == null) {
                throw ex;
            }
            ensureMembership(tenantId, raced.getId(), userId);
            if (!selfChat) {
                ensureMembership(tenantId, raced.getId(), peerUserId);
            }
            return raced.getId();
        }

        createMember(tenantId, conversation.getId(), userId);
        if (!selfChat) {
            createMember(tenantId, conversation.getId(), peerUserId);
        }
        return conversation.getId();
    }

    @Override
    public void requireMembership(Long tenantId, Long conversationId, Long userId) {
        ImConversationMemberDO member = conversationMemberMapper.selectOne(
                Wrappers.<ImConversationMemberDO>lambdaQuery()
                        .eq(ImConversationMemberDO::getTenantId, tenantId)
                        .eq(ImConversationMemberDO::getConversationId, conversationId)
                        .eq(ImConversationMemberDO::getSubjectType, ImMemberSubjectType.USER)
                        .eq(ImConversationMemberDO::getSubjectId, userId));
        if (member == null) {
            throw new ServiceException(ErrorCodeConstants.CONVERSATION_ACCESS_DENIED);
        }
    }

    @Override
    public void lockConversation(Long tenantId, Long conversationId) {
        ImConversationDO conversation = conversationMapper.selectOne(
                Wrappers.<ImConversationDO>lambdaQuery()
                        .eq(ImConversationDO::getTenantId, tenantId)
                        .eq(ImConversationDO::getId, conversationId)
                        .last("FOR UPDATE"));
        if (conversation == null) {
            throw new ServiceException(ErrorCodeConstants.CONVERSATION_NOT_FOUND);
        }
    }

    @Override
    @Transactional
    public void markConversationRead(Long conversationId, Long readSeq) {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        markConversationRead(loginUser.getTenantId(), loginUser.getUserId(), conversationId, readSeq);
    }

    private void markConversationRead(Long tenantId, Long userId, Long conversationId, Long readSeq) {
        requireMembership(tenantId, conversationId, userId);
        ImConversationMemberDO member = conversationMemberMapper.selectOne(
                Wrappers.<ImConversationMemberDO>lambdaQuery()
                        .eq(ImConversationMemberDO::getTenantId, tenantId)
                        .eq(ImConversationMemberDO::getConversationId, conversationId)
                        .eq(ImConversationMemberDO::getSubjectType, ImMemberSubjectType.USER)
                        .eq(ImConversationMemberDO::getSubjectId, userId));
        if (member == null) {
            throw new ServiceException(ErrorCodeConstants.CONVERSATION_ACCESS_DENIED);
        }

        long targetReadSeq = readSeq != null ? readSeq : 0L;
        long currentReadSeq = member.getReadSeq() != null ? member.getReadSeq() : 0L;
        long newReadSeq = Math.max(currentReadSeq, targetReadSeq);

        Long unreadCount = messageMapper.selectCount(
                Wrappers.<ImMessageDO>lambdaQuery()
                        .eq(ImMessageDO::getTenantId, tenantId)
                        .eq(ImMessageDO::getConversationId, conversationId)
                        .gt(ImMessageDO::getSeq, newReadSeq));

        member.setReadSeq(newReadSeq);
        member.setUnreadCount(unreadCount != null ? unreadCount.intValue() : 0);
        conversationMemberMapper.updateById(member);

        if (newReadSeq > currentReadSeq) {
            dispatchReadUpdated(tenantId, conversationId, userId, newReadSeq);
        }
    }

    @Override
    public ConversationReadStatusRespVO getReadStatus(Long conversationId) {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        return getReadStatus(loginUser.getTenantId(), loginUser.getUserId(), conversationId);
    }

    private ConversationReadStatusRespVO getReadStatus(Long tenantId, Long userId, Long conversationId) {
        requireMembership(tenantId, conversationId, userId);
        List<ImConversationMemberDO> members = conversationMemberMapper.selectList(
                Wrappers.<ImConversationMemberDO>lambdaQuery()
                        .eq(ImConversationMemberDO::getTenantId, tenantId)
                        .eq(ImConversationMemberDO::getConversationId, conversationId));

        ConversationReadStatusRespVO response = new ConversationReadStatusRespVO();
        response.setConversationId(conversationId);
        response.setMembers(members.stream()
                .filter(member -> ImMemberSubjectType.USER.equals(member.getSubjectType()))
                .map(member -> {
                    ConversationMemberReadStatusRespVO item = new ConversationMemberReadStatusRespVO();
                    item.setUserId(member.getSubjectId());
                    item.setReadSeq(member.getReadSeq() != null ? member.getReadSeq() : 0L);
                    return item;
                }).toList());
        return response;
    }

    private void dispatchReadUpdated(Long tenantId, Long conversationId, Long userId, Long readSeq) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("conversationId", conversationId);
        payload.put("userId", userId);
        payload.put("readSeq", readSeq);

        RealtimeEnvelopeDTO envelope = RealtimeEnvelopeDTO.builder()
                .domain(ImRealtimeTypes.DOMAIN)
                .type(ImRealtimeTypes.READ_UPDATED)
                .ts(System.currentTimeMillis())
                .payload(payload)
                .build();

        List<Long> recipients = listOtherMemberUserIds(tenantId, conversationId, userId);
        if (!recipients.isEmpty()) {
            realtimeTransportApi.sendToUsers(tenantId, recipients, envelope);
        }
    }

    @Override
    public List<Long> listOtherMemberUserIds(Long tenantId, Long conversationId, Long senderId) {
        return conversationMemberMapper.selectList(
                        Wrappers.<ImConversationMemberDO>lambdaQuery()
                                .eq(ImConversationMemberDO::getTenantId, tenantId)
                                .eq(ImConversationMemberDO::getConversationId, conversationId)
                                .eq(ImConversationMemberDO::getSubjectType, ImMemberSubjectType.USER))
                .stream()
                .map(ImConversationMemberDO::getSubjectId)
                .filter(userId -> !Objects.equals(userId, senderId))
                .toList();
    }

    @Override
    public List<Long> listMemberUserIds(Long tenantId, Long conversationId) {
        return conversationMemberMapper.selectList(
                        Wrappers.<ImConversationMemberDO>lambdaQuery()
                                .eq(ImConversationMemberDO::getTenantId, tenantId)
                                .eq(ImConversationMemberDO::getConversationId, conversationId)
                                .eq(ImConversationMemberDO::getSubjectType, ImMemberSubjectType.USER))
                .stream()
                .map(ImConversationMemberDO::getSubjectId)
                .toList();
    }

    @Override
    public ImConversationDO requireConversation(Long tenantId, Long conversationId) {
        ImConversationDO conversation = conversationMapper.selectOne(
                Wrappers.<ImConversationDO>lambdaQuery()
                        .eq(ImConversationDO::getTenantId, tenantId)
                        .eq(ImConversationDO::getId, conversationId));
        if (conversation == null) {
            throw new ServiceException(ErrorCodeConstants.CONVERSATION_NOT_FOUND);
        }
        return conversation;
    }

    private Map<Long, Long> loadPeerUserIds(Long tenantId, Long userId, List<ImConversationDO> conversations) {
        if (conversations.isEmpty()) {
            return Map.of();
        }
        List<Long> conversationIds = conversations.stream().map(ImConversationDO::getId).toList();
        List<ImConversationMemberDO> allMembers = conversationMemberMapper.selectList(
                Wrappers.<ImConversationMemberDO>lambdaQuery()
                        .eq(ImConversationMemberDO::getTenantId, tenantId)
                        .eq(ImConversationMemberDO::getSubjectType, ImMemberSubjectType.USER)
                        .in(ImConversationMemberDO::getConversationId, conversationIds));

        Map<Long, List<Long>> membersByConversation = new HashMap<>();
        for (ImConversationMemberDO member : allMembers) {
            membersByConversation
                    .computeIfAbsent(member.getConversationId(), key -> new ArrayList<>())
                    .add(member.getSubjectId());
        }

        Map<Long, Long> peerByConversation = new HashMap<>();
        for (ImConversationDO conversation : conversations) {
            List<Long> memberIds = membersByConversation.getOrDefault(conversation.getId(), List.of());
            Long peerUserId = memberIds.stream()
                    .filter(memberId -> !Objects.equals(memberId, userId))
                    .findFirst()
                    .orElse(null);
            // Self-DIRECT: single member, pair key low==high==me.
            if (peerUserId == null
                    && Objects.equals(conversation.getDirectPeerLow(), conversation.getDirectPeerHigh())
                    && Objects.equals(conversation.getDirectPeerLow(), userId)) {
                peerUserId = userId;
            }
            if (peerUserId != null) {
                peerByConversation.put(conversation.getId(), peerUserId);
            }
        }
        return peerByConversation;
    }

    private Map<Long, ImGroupDO> loadGroups(Long tenantId, List<ImConversationDO> groupConversations) {
        if (groupConversations.isEmpty()) {
            return Map.of();
        }
        List<Long> conversationIds = groupConversations.stream().map(ImConversationDO::getId).toList();
        List<ImGroupDO> groups = groupMapper.selectList(
                Wrappers.<ImGroupDO>lambdaQuery()
                        .eq(ImGroupDO::getTenantId, tenantId)
                        .in(ImGroupDO::getConversationId, conversationIds));
        Map<Long, ImGroupDO> groupByConversationId = new HashMap<>();
        for (ImGroupDO group : groups) {
            groupByConversationId.put(group.getConversationId(), group);
        }
        return groupByConversationId;
    }

    private Map<Long, ImBotDO> loadBots(List<ImConversationDO> botDmConversations) {
        if (botDmConversations.isEmpty()) {
            return Map.of();
        }
        List<Long> botIds = botDmConversations.stream()
                .map(ImConversationDO::getBotPeerBotId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (botIds.isEmpty()) {
            return Map.of();
        }
        return botMapper.selectBatchIds(botIds).stream()
                .collect(Collectors.toMap(ImBotDO::getId, bot -> bot, (left, right) -> left));
    }

    private Map<Long, Integer> loadMemberCounts(Long tenantId, List<ImConversationDO> groupConversations) {
        if (groupConversations.isEmpty()) {
            return Map.of();
        }
        List<Long> conversationIds = groupConversations.stream().map(ImConversationDO::getId).toList();
        List<ImConversationMemberDO> members = conversationMemberMapper.selectList(
                Wrappers.<ImConversationMemberDO>lambdaQuery()
                        .eq(ImConversationMemberDO::getTenantId, tenantId)
                        .in(ImConversationMemberDO::getConversationId, conversationIds));
        Map<Long, Integer> counts = new HashMap<>();
        for (ImConversationMemberDO member : members) {
            counts.merge(member.getConversationId(), 1, Integer::sum);
        }
        return counts;
    }

    private boolean matchesKeyword(ConversationListItem item, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        return containsIgnoreCase(item.getTitle(), keyword)
                || containsIgnoreCase(item.getLastMsgPreview(), keyword);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return StringUtils.hasText(value) && value.toLowerCase().contains(keyword);
    }

    private void ensureMembership(Long tenantId, Long conversationId, Long userId) {
        Long count = conversationMemberMapper.selectCount(
                Wrappers.<ImConversationMemberDO>lambdaQuery()
                        .eq(ImConversationMemberDO::getTenantId, tenantId)
                        .eq(ImConversationMemberDO::getConversationId, conversationId)
                        .eq(ImConversationMemberDO::getSubjectType, ImMemberSubjectType.USER)
                        .eq(ImConversationMemberDO::getSubjectId, userId));
        if (count == null || count == 0) {
            createMember(tenantId, conversationId, userId);
        }
    }

    private void createMember(Long tenantId, Long conversationId, Long userId) {
        ImConversationMemberDO member = new ImConversationMemberDO();
        member.setTenantId(tenantId);
        member.setConversationId(conversationId);
        member.setSubjectType(ImMemberSubjectType.USER);
        member.setSubjectId(userId);
        member.setRole("member");
        member.setReadSeq(0L);
        member.setUnreadCount(0);
        member.setJoinTime(OffsetDateTime.now());
        member.setPinned(0);
        member.setCreator(userId);
        conversationMemberMapper.insert(member);
    }
}
