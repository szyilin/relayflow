package com.relayflow.module.im.service.bot;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.tenant.core.TenantContextHolder;
import com.relayflow.module.im.api.bot.dto.ImBotProvisionAgentCarrierCommand;
import com.relayflow.module.im.api.bot.dto.ImBotProvisionResult;
import com.relayflow.module.im.api.bot.dto.ImBotRetireAgentCarrierCommand;
import com.relayflow.module.im.api.bot.dto.ImBotUpdateAgentCarrierProfileCommand;
import com.relayflow.module.im.dal.dataobject.ImBotDO;
import com.relayflow.module.im.dal.dataobject.ImBotTenantEnablementDO;
import com.relayflow.module.im.dal.dataobject.ImBotUserEnablementDO;
import com.relayflow.module.im.dal.dataobject.ImConversationDO;
import com.relayflow.module.im.dal.dataobject.ImConversationMemberDO;
import com.relayflow.module.im.dal.mapper.ImBotMapper;
import com.relayflow.module.im.dal.mapper.ImBotTenantEnablementMapper;
import com.relayflow.module.im.dal.mapper.ImBotUserEnablementMapper;
import com.relayflow.module.im.dal.mapper.ImConversationMapper;
import com.relayflow.module.im.dal.mapper.ImConversationMemberMapper;
import com.relayflow.module.im.enums.ErrorCodeConstants;
import com.relayflow.module.im.enums.ImBotEnablePolicy;
import com.relayflow.module.im.enums.ImBotHandlerKind;
import com.relayflow.module.im.enums.ImBotType;
import com.relayflow.module.im.enums.ImConversationType;
import com.relayflow.module.im.enums.ImMemberSubjectType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ImBotProvisioningServiceImpl {

    private static final int BOT_STATUS_ENABLED = 1;
    private static final int BOT_STATUS_DISABLED = 0;
    private static final int TENANT_ENABLED = 1;

    private final ImBotMapper botMapper;
    private final ImBotTenantEnablementMapper tenantEnablementMapper;
    private final ImBotUserEnablementMapper userEnablementMapper;
    private final ImConversationMapper conversationMapper;
    private final ImConversationMemberMapper conversationMemberMapper;

    @Transactional
    public ImBotProvisionResult provisionAgentCarrier(ImBotProvisionAgentCarrierCommand command) {
        validateProvision(command);
        Long tenantId = command.getTenantId();
        Long agentId = command.getAgentId();
        Long previous = TenantContextHolder.get();
        try {
            TenantContextHolder.set(tenantId);
            ImBotDO bot = botMapper.selectOne(Wrappers.<ImBotDO>lambdaQuery()
                    .eq(ImBotDO::getOwnerAgentId, agentId));
            if (bot == null) {
                bot = createCarrierBot(command);
            } else {
                applyProfile(bot, command.getName(), command.getDescription(), command.getAvatarFileId());
                bot.setStatus(BOT_STATUS_ENABLED);
                bot.setType(ImBotType.TENANT);
                bot.setHandlerKind(ImBotHandlerKind.PLATFORM);
                if (command.getOwnerUserId() != null) {
                    bot.setOwnerUserId(command.getOwnerUserId());
                }
                botMapper.updateById(bot);
            }
            ensureTenantEnablement(tenantId, bot.getId());
            if (command.getOwnerUserId() != null) {
                ensureUserEnablement(tenantId, command.getOwnerUserId(), bot.getId());
                ensureBotDm(tenantId, bot.getId(), command.getOwnerUserId());
            }
            return toResult(bot);
        } finally {
            restoreTenant(previous);
        }
    }

    @Transactional
    public void retireAgentCarrier(ImBotRetireAgentCarrierCommand command) {
        if (command == null || command.getTenantId() == null) {
            throw new ServiceException(ErrorCodeConstants.BOT_PROVISION_INVALID);
        }
        Long previous = TenantContextHolder.get();
        try {
            TenantContextHolder.set(command.getTenantId());
            ImBotDO bot = findCarrier(command.getAgentId(), command.getBotId());
            if (bot == null) {
                return;
            }
            bot.setStatus(BOT_STATUS_DISABLED);
            botMapper.updateById(bot);
            softDeleteEnablements(bot.getId());
        } finally {
            restoreTenant(previous);
        }
    }

    @Transactional
    public ImBotProvisionResult updateAgentCarrierProfile(ImBotUpdateAgentCarrierProfileCommand command) {
        if (command == null || command.getTenantId() == null || command.getAgentId() == null
                || !StringUtils.hasText(command.getName())) {
            throw new ServiceException(ErrorCodeConstants.BOT_PROVISION_INVALID);
        }
        Long previous = TenantContextHolder.get();
        try {
            TenantContextHolder.set(command.getTenantId());
            ImBotDO bot = botMapper.selectOne(Wrappers.<ImBotDO>lambdaQuery()
                    .eq(ImBotDO::getOwnerAgentId, command.getAgentId()));
            if (bot == null || !Objects.equals(bot.getStatus(), BOT_STATUS_ENABLED)) {
                throw new ServiceException(ErrorCodeConstants.BOT_NOT_FOUND);
            }
            applyProfile(bot, command.getName(), command.getDescription(), command.getAvatarFileId());
            botMapper.updateById(bot);
            return toResult(bot);
        } finally {
            restoreTenant(previous);
        }
    }

    private ImBotDO createCarrierBot(ImBotProvisionAgentCarrierCommand command) {
        ImBotDO bot = new ImBotDO();
        bot.setCode(carrierCode(command.getAgentId()));
        bot.setName(command.getName().trim());
        bot.setDescription(StringUtils.hasText(command.getDescription()) ? command.getDescription().trim() : null);
        bot.setAvatarFileId(command.getAvatarFileId());
        bot.setScope("tenant");
        bot.setType(ImBotType.TENANT);
        bot.setEnablePolicy(ImBotEnablePolicy.DEFAULT_ON);
        bot.setHandlerKind(ImBotHandlerKind.PLATFORM);
        bot.setStatus(BOT_STATUS_ENABLED);
        bot.setOwnerAgentId(command.getAgentId());
        bot.setOwnerUserId(command.getOwnerUserId());
        try {
            botMapper.insert(bot);
        } catch (DuplicateKeyException ex) {
            ImBotDO raced = botMapper.selectOne(Wrappers.<ImBotDO>lambdaQuery()
                    .eq(ImBotDO::getOwnerAgentId, command.getAgentId()));
            if (raced == null) {
                raced = botMapper.selectOne(Wrappers.<ImBotDO>lambdaQuery()
                        .eq(ImBotDO::getCode, carrierCode(command.getAgentId())));
            }
            if (raced == null) {
                throw ex;
            }
            applyProfile(raced, command.getName(), command.getDescription(), command.getAvatarFileId());
            raced.setStatus(BOT_STATUS_ENABLED);
            raced.setOwnerAgentId(command.getAgentId());
            if (command.getOwnerUserId() != null) {
                raced.setOwnerUserId(command.getOwnerUserId());
            }
            raced.setType(ImBotType.TENANT);
            raced.setHandlerKind(ImBotHandlerKind.PLATFORM);
            botMapper.updateById(raced);
            return raced;
        }
        return bot;
    }

    private ImBotDO findCarrier(Long agentId, Long botId) {
        if (agentId != null) {
            return botMapper.selectOne(Wrappers.<ImBotDO>lambdaQuery()
                    .eq(ImBotDO::getOwnerAgentId, agentId));
        }
        if (botId != null) {
            ImBotDO bot = botMapper.selectById(botId);
            if (bot != null && bot.getOwnerAgentId() != null) {
                return bot;
            }
        }
        return null;
    }

    private void softDeleteEnablements(Long botId) {
        for (ImBotTenantEnablementDO row : tenantEnablementMapper.selectList(
                Wrappers.<ImBotTenantEnablementDO>lambdaQuery().eq(ImBotTenantEnablementDO::getBotId, botId))) {
            tenantEnablementMapper.deleteById(row.getId());
        }
        for (ImBotUserEnablementDO row : userEnablementMapper.selectList(
                Wrappers.<ImBotUserEnablementDO>lambdaQuery().eq(ImBotUserEnablementDO::getBotId, botId))) {
            userEnablementMapper.deleteById(row.getId());
        }
    }

    private void ensureTenantEnablement(Long tenantId, Long botId) {
        ImBotTenantEnablementDO existing = tenantEnablementMapper.selectOne(
                Wrappers.<ImBotTenantEnablementDO>lambdaQuery()
                        .eq(ImBotTenantEnablementDO::getTenantId, tenantId)
                        .eq(ImBotTenantEnablementDO::getBotId, botId));
        if (existing != null) {
            if (!Objects.equals(existing.getEnabled(), TENANT_ENABLED)) {
                existing.setEnabled(TENANT_ENABLED);
                tenantEnablementMapper.updateById(existing);
            }
            return;
        }
        ImBotTenantEnablementDO row = new ImBotTenantEnablementDO();
        row.setTenantId(tenantId);
        row.setBotId(botId);
        row.setEnabled(TENANT_ENABLED);
        try {
            tenantEnablementMapper.insert(row);
        } catch (DuplicateKeyException ignored) {
            // concurrent
        }
    }

    private void ensureUserEnablement(Long tenantId, Long userId, Long botId) {
        Long count = userEnablementMapper.selectCount(
                Wrappers.<ImBotUserEnablementDO>lambdaQuery()
                        .eq(ImBotUserEnablementDO::getTenantId, tenantId)
                        .eq(ImBotUserEnablementDO::getUserId, userId)
                        .eq(ImBotUserEnablementDO::getBotId, botId));
        if (count != null && count > 0) {
            return;
        }
        ImBotUserEnablementDO row = new ImBotUserEnablementDO();
        row.setTenantId(tenantId);
        row.setUserId(userId);
        row.setBotId(botId);
        try {
            userEnablementMapper.insert(row);
        } catch (DuplicateKeyException ignored) {
            // concurrent
        }
    }

    private void ensureBotDm(Long tenantId, Long botId, Long userId) {
        ImConversationDO existing = conversationMapper.selectOne(
                Wrappers.<ImConversationDO>lambdaQuery()
                        .eq(ImConversationDO::getTenantId, tenantId)
                        .eq(ImConversationDO::getType, ImConversationType.BOT_DM)
                        .eq(ImConversationDO::getBotPeerBotId, botId)
                        .eq(ImConversationDO::getBotPeerUserId, userId));
        if (existing != null) {
            ensureSubjectMember(tenantId, existing.getId(), ImMemberSubjectType.USER, userId);
            ensureSubjectMember(tenantId, existing.getId(), ImMemberSubjectType.BOT, botId);
            return;
        }
        ImConversationDO conversation = new ImConversationDO();
        conversation.setTenantId(tenantId);
        conversation.setType(ImConversationType.BOT_DM);
        conversation.setBotPeerBotId(botId);
        conversation.setBotPeerUserId(userId);
        try {
            conversationMapper.insert(conversation);
        } catch (DuplicateKeyException ex) {
            ImConversationDO raced = conversationMapper.selectOne(
                    Wrappers.<ImConversationDO>lambdaQuery()
                            .eq(ImConversationDO::getTenantId, tenantId)
                            .eq(ImConversationDO::getType, ImConversationType.BOT_DM)
                            .eq(ImConversationDO::getBotPeerBotId, botId)
                            .eq(ImConversationDO::getBotPeerUserId, userId));
            if (raced == null) {
                throw ex;
            }
            ensureSubjectMember(tenantId, raced.getId(), ImMemberSubjectType.USER, userId);
            ensureSubjectMember(tenantId, raced.getId(), ImMemberSubjectType.BOT, botId);
            return;
        }
        insertMember(tenantId, conversation.getId(), ImMemberSubjectType.USER, userId);
        insertMember(tenantId, conversation.getId(), ImMemberSubjectType.BOT, botId);
    }

    private void ensureSubjectMember(Long tenantId, Long conversationId, String subjectType, Long subjectId) {
        Long count = conversationMemberMapper.selectCount(
                Wrappers.<ImConversationMemberDO>lambdaQuery()
                        .eq(ImConversationMemberDO::getTenantId, tenantId)
                        .eq(ImConversationMemberDO::getConversationId, conversationId)
                        .eq(ImConversationMemberDO::getSubjectType, subjectType)
                        .eq(ImConversationMemberDO::getSubjectId, subjectId));
        if (count == null || count == 0) {
            insertMember(tenantId, conversationId, subjectType, subjectId);
        }
    }

    private void insertMember(Long tenantId, Long conversationId, String subjectType, Long subjectId) {
        ImConversationMemberDO member = new ImConversationMemberDO();
        member.setTenantId(tenantId);
        member.setConversationId(conversationId);
        member.setSubjectType(subjectType);
        member.setSubjectId(subjectId);
        member.setRole("member");
        member.setReadSeq(0L);
        member.setUnreadCount(0);
        member.setJoinTime(OffsetDateTime.now());
        member.setPinned(0);
        conversationMemberMapper.insert(member);
    }

    private static void applyProfile(ImBotDO bot, String name, String description, Long avatarFileId) {
        bot.setName(name.trim());
        bot.setDescription(StringUtils.hasText(description) ? description.trim() : null);
        if (avatarFileId != null) {
            bot.setAvatarFileId(avatarFileId);
        }
    }

    private static void validateProvision(ImBotProvisionAgentCarrierCommand command) {
        if (command == null || command.getTenantId() == null || command.getAgentId() == null
                || !StringUtils.hasText(command.getName())) {
            throw new ServiceException(ErrorCodeConstants.BOT_PROVISION_INVALID);
        }
    }

    private static ImBotProvisionResult toResult(ImBotDO bot) {
        ImBotProvisionResult result = new ImBotProvisionResult();
        result.setBotId(bot.getId());
        result.setBotCode(bot.getCode());
        return result;
    }

    static String carrierCode(Long agentId) {
        return "agent-carrier-" + agentId;
    }

    private static void restoreTenant(Long previous) {
        if (previous != null) {
            TenantContextHolder.set(previous);
        } else {
            TenantContextHolder.clear();
        }
    }
}
