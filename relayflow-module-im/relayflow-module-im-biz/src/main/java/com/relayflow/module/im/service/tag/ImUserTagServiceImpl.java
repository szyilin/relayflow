package com.relayflow.module.im.service.tag;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.im.controller.app.vo.ImUserTagItemRespVO;
import com.relayflow.module.im.dal.dataobject.ImConversationUserTagDO;
import com.relayflow.module.im.dal.dataobject.ImUserTagDO;
import com.relayflow.module.im.dal.mapper.ImConversationUserTagMapper;
import com.relayflow.module.im.dal.mapper.ImUserTagMapper;
import com.relayflow.module.im.enums.ErrorCodeConstants;
import com.relayflow.module.im.service.conversation.ImConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImUserTagServiceImpl implements ImUserTagService {

    private final ImUserTagMapper userTagMapper;
    private final ImConversationUserTagMapper conversationUserTagMapper;
    private final ImConversationService conversationService;

    @Override
    public List<ImUserTagItemRespVO> listMyTags() {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        return userTagMapper.selectList(
                        Wrappers.<ImUserTagDO>lambdaQuery()
                                .eq(ImUserTagDO::getTenantId, loginUser.getTenantId())
                                .eq(ImUserTagDO::getUserId, loginUser.getUserId())
                                .orderByAsc(ImUserTagDO::getName))
                .stream()
                .map(this::toItem)
                .toList();
    }

    @Override
    @Transactional
    public ImUserTagItemRespVO createTag(String name) {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        return toItem(createTagInternal(loginUser.getTenantId(), loginUser.getUserId(), name));
    }

    @Override
    @Transactional
    public void assignTag(Long conversationId, Long tagId) {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        Long tenantId = loginUser.getTenantId();
        Long userId = loginUser.getUserId();
        conversationService.requireMembership(tenantId, conversationId, userId);
        requireOwnedTag(tenantId, userId, tagId);
        insertAssignmentIdempotent(tenantId, userId, conversationId, tagId);
    }

    @Override
    @Transactional
    public ImUserTagItemRespVO createAndAssign(Long conversationId, String name) {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        Long tenantId = loginUser.getTenantId();
        Long userId = loginUser.getUserId();
        conversationService.requireMembership(tenantId, conversationId, userId);
        ImUserTagDO tag = createTagInternal(tenantId, userId, name);
        insertAssignmentIdempotent(tenantId, userId, conversationId, tag.getId());
        return toItem(tag);
    }

    @Override
    public Map<Long, List<Long>> listTagIdsByConversation(Long tenantId, Long userId,
                                                          Collection<Long> conversationIds) {
        Map<Long, List<Long>> result = new HashMap<>();
        if (conversationIds == null || conversationIds.isEmpty()) {
            return result;
        }
        List<ImConversationUserTagDO> rows = conversationUserTagMapper.selectList(
                Wrappers.<ImConversationUserTagDO>lambdaQuery()
                        .eq(ImConversationUserTagDO::getTenantId, tenantId)
                        .eq(ImConversationUserTagDO::getUserId, userId)
                        .in(ImConversationUserTagDO::getConversationId, conversationIds));
        for (ImConversationUserTagDO row : rows) {
            result.computeIfAbsent(row.getConversationId(), ignored -> new ArrayList<>()).add(row.getTagId());
        }
        return result;
    }

    @Override
    public ImUserTagDO requireOwnedTag(Long tenantId, Long userId, Long tagId) {
        ImUserTagDO tag = userTagMapper.selectOne(
                Wrappers.<ImUserTagDO>lambdaQuery()
                        .eq(ImUserTagDO::getTenantId, tenantId)
                        .eq(ImUserTagDO::getUserId, userId)
                        .eq(ImUserTagDO::getId, tagId));
        if (tag == null) {
            throw new ServiceException(ErrorCodeConstants.TAG_NOT_FOUND);
        }
        return tag;
    }

    private ImUserTagDO createTagInternal(Long tenantId, Long userId, String name) {
        String trimmed = name == null ? "" : name.trim();
        if (!StringUtils.hasText(trimmed) || trimmed.length() > 64) {
            throw new ServiceException(ErrorCodeConstants.TAG_NAME_INVALID);
        }
        ImUserTagDO existing = userTagMapper.selectOne(
                Wrappers.<ImUserTagDO>lambdaQuery()
                        .eq(ImUserTagDO::getTenantId, tenantId)
                        .eq(ImUserTagDO::getUserId, userId)
                        .eq(ImUserTagDO::getName, trimmed));
        if (existing != null) {
            return existing;
        }
        ImUserTagDO tag = new ImUserTagDO();
        tag.setTenantId(tenantId);
        tag.setUserId(userId);
        tag.setName(trimmed);
        tag.setCreator(userId);
        try {
            userTagMapper.insert(tag);
            return tag;
        } catch (DuplicateKeyException ex) {
            ImUserTagDO raced = userTagMapper.selectOne(
                    Wrappers.<ImUserTagDO>lambdaQuery()
                            .eq(ImUserTagDO::getTenantId, tenantId)
                            .eq(ImUserTagDO::getUserId, userId)
                            .eq(ImUserTagDO::getName, trimmed));
            if (raced != null) {
                return raced;
            }
            throw ex;
        }
    }

    private void insertAssignmentIdempotent(Long tenantId, Long userId, Long conversationId, Long tagId) {
        Long count = conversationUserTagMapper.selectCount(
                Wrappers.<ImConversationUserTagDO>lambdaQuery()
                        .eq(ImConversationUserTagDO::getTenantId, tenantId)
                        .eq(ImConversationUserTagDO::getUserId, userId)
                        .eq(ImConversationUserTagDO::getConversationId, conversationId)
                        .eq(ImConversationUserTagDO::getTagId, tagId));
        if (count != null && count > 0) {
            return;
        }
        ImConversationUserTagDO row = new ImConversationUserTagDO();
        row.setTenantId(tenantId);
        row.setUserId(userId);
        row.setConversationId(conversationId);
        row.setTagId(tagId);
        row.setCreator(userId);
        try {
            conversationUserTagMapper.insert(row);
        } catch (DuplicateKeyException ignored) {
            // idempotent
        }
    }

    private ImUserTagItemRespVO toItem(ImUserTagDO tag) {
        ImUserTagItemRespVO vo = new ImUserTagItemRespVO();
        vo.setId(tag.getId());
        vo.setName(tag.getName());
        return vo;
    }
}
