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
        Long tenantId = loginUser.getTenantId();
        Long userId = loginUser.getUserId();
        List<ImUserTagDO> tags = userTagMapper.selectList(
                Wrappers.<ImUserTagDO>lambdaQuery()
                        .eq(ImUserTagDO::getTenantId, tenantId)
                        .eq(ImUserTagDO::getUserId, userId)
                        .orderByAsc(ImUserTagDO::getName));
        Map<Long, Integer> usageByTag = countAssignmentsByTag(tenantId, userId);
        return tags.stream()
                .map(tag -> toItem(tag, usageByTag.getOrDefault(tag.getId(), 0)))
                .toList();
    }

    @Override
    @Transactional
    public ImUserTagItemRespVO createTag(String name) {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        return toItem(createTagInternal(loginUser.getTenantId(), loginUser.getUserId(), name), 0);
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
    public void unassignTag(Long conversationId, Long tagId) {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        Long tenantId = loginUser.getTenantId();
        Long userId = loginUser.getUserId();
        conversationService.requireMembership(tenantId, conversationId, userId);
        requireOwnedTag(tenantId, userId, tagId);
        conversationUserTagMapper.delete(
                Wrappers.<ImConversationUserTagDO>lambdaQuery()
                        .eq(ImConversationUserTagDO::getTenantId, tenantId)
                        .eq(ImConversationUserTagDO::getUserId, userId)
                        .eq(ImConversationUserTagDO::getConversationId, conversationId)
                        .eq(ImConversationUserTagDO::getTagId, tagId));
    }

    @Override
    @Transactional
    public void deleteTag(Long tagId) {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        Long tenantId = loginUser.getTenantId();
        Long userId = loginUser.getUserId();
        requireOwnedTag(tenantId, userId, tagId);
        conversationUserTagMapper.delete(
                Wrappers.<ImConversationUserTagDO>lambdaQuery()
                        .eq(ImConversationUserTagDO::getTenantId, tenantId)
                        .eq(ImConversationUserTagDO::getUserId, userId)
                        .eq(ImConversationUserTagDO::getTagId, tagId));
        userTagMapper.delete(
                Wrappers.<ImUserTagDO>lambdaQuery()
                        .eq(ImUserTagDO::getTenantId, tenantId)
                        .eq(ImUserTagDO::getUserId, userId)
                        .eq(ImUserTagDO::getId, tagId));
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
        return toItem(tag, 1);
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

    private Map<Long, Integer> countAssignmentsByTag(Long tenantId, Long userId) {
        List<ImConversationUserTagDO> rows = conversationUserTagMapper.selectList(
                Wrappers.<ImConversationUserTagDO>lambdaQuery()
                        .select(ImConversationUserTagDO::getTagId)
                        .eq(ImConversationUserTagDO::getTenantId, tenantId)
                        .eq(ImConversationUserTagDO::getUserId, userId));
        Map<Long, Integer> counts = new HashMap<>();
        for (ImConversationUserTagDO row : rows) {
            if (row.getTagId() == null) {
                continue;
            }
            counts.merge(row.getTagId(), 1, Integer::sum);
        }
        return counts;
    }

    private ImUserTagItemRespVO toItem(ImUserTagDO tag, int conversationCount) {
        ImUserTagItemRespVO vo = new ImUserTagItemRespVO();
        vo.setId(tag.getId());
        vo.setName(tag.getName());
        vo.setConversationCount(conversationCount);
        return vo;
    }
}
