package com.relayflow.module.im.service.sticker;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.framework.security.core.LoginUser;
import com.relayflow.framework.security.core.SecurityFrameworkUtils;
import com.relayflow.module.im.controller.app.vo.AddUserStickerFromMessageReqVO;
import com.relayflow.module.im.controller.app.vo.AddUserStickerReqVO;
import com.relayflow.module.im.controller.app.vo.ContentBlockVO;
import com.relayflow.module.im.controller.app.vo.DeleteUserStickersReqVO;
import com.relayflow.module.im.controller.app.vo.MessageContentVO;
import com.relayflow.module.im.controller.app.vo.ReorderUserStickersReqVO;
import com.relayflow.module.im.controller.app.vo.UserStickerItemRespVO;
import com.relayflow.module.im.dal.dataobject.ImMessageDO;
import com.relayflow.module.im.dal.dataobject.ImUserStickerDO;
import com.relayflow.module.im.dal.mapper.ImMessageMapper;
import com.relayflow.module.im.dal.mapper.ImUserStickerMapper;
import com.relayflow.module.im.enums.ErrorCodeConstants;
import com.relayflow.module.im.service.conversation.ImConversationService;
import com.relayflow.module.im.service.message.ImContentHelper;
import com.relayflow.module.infra.api.file.FileApi;
import com.relayflow.module.infra.api.file.dto.FileRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImUserStickerService {

    private static final int MAX_PER_USER = 200;
    private static final long MAX_FILE_BYTES = 2L * 1024 * 1024;
    private static final String APP_FILE_DOWNLOAD_PREFIX = "/app-api/infra/file/download?fileId=";

    private final ImUserStickerMapper userStickerMapper;
    private final ImMessageMapper messageMapper;
    private final ImConversationService conversationService;
    private final ImContentHelper contentHelper;
    private final FileApi fileApi;

    public List<UserStickerItemRespVO> listMine() {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        List<ImUserStickerDO> rows = userStickerMapper.selectList(
                Wrappers.<ImUserStickerDO>lambdaQuery()
                        .eq(ImUserStickerDO::getTenantId, loginUser.getTenantId())
                        .eq(ImUserStickerDO::getUserId, loginUser.getUserId())
                        .orderByAsc(ImUserStickerDO::getSort)
                        .orderByAsc(ImUserStickerDO::getId));
        return rows.stream().map(this::toItem).toList();
    }

    @Transactional
    public UserStickerItemRespVO add(AddUserStickerReqVO request) {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        Long fileId = request.getFileId();
        requireImageFile(fileId);
        return upsertOwned(loginUser, fileId, request.getName(), null);
    }

    @Transactional
    public UserStickerItemRespVO addFromMessage(AddUserStickerFromMessageReqVO request) {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        Long tenantId = loginUser.getTenantId();
        Long userId = loginUser.getUserId();
        ImMessageDO message = messageMapper.selectById(request.getMessageId());
        if (message == null || !Objects.equals(message.getTenantId(), tenantId)) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_NOT_FOUND);
        }
        if (message.getRecalledAt() != null) {
            throw new ServiceException(ErrorCodeConstants.STICKER_COLLECT_DENIED);
        }
        conversationService.requireMembership(tenantId, message.getConversationId(), userId);
        if (!ImContentHelper.MESSAGE_TYPE_STICKER.equals(message.getType())) {
            throw new ServiceException(ErrorCodeConstants.STICKER_COLLECT_DENIED);
        }
        MessageContentVO content = contentHelper.fromJson(message.getContentJson());
        ContentBlockVO sticker = contentHelper.firstStickerBlock(content);
        if (sticker == null || !StringUtils.hasText(sticker.getFileId())) {
            throw new ServiceException(ErrorCodeConstants.STICKER_COLLECT_DENIED);
        }
        Long fileId;
        try {
            fileId = Long.parseLong(sticker.getFileId().trim());
        } catch (NumberFormatException ex) {
            throw new ServiceException(ErrorCodeConstants.STICKER_COLLECT_DENIED);
        }
        requireImageFile(fileId);
        String name = StringUtils.hasText(sticker.getText()) ? sticker.getText() : sticker.getEmoji();
        return upsertOwned(loginUser, fileId, name, message.getId());
    }

    @Transactional
    public void reorder(ReorderUserStickersReqVO request) {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        List<Long> ids = request.getIds();
        if (CollectionUtils.isEmpty(ids)) {
            throw new ServiceException(ErrorCodeConstants.STICKER_NOT_FOUND);
        }
        Set<Long> unique = new HashSet<>(ids);
        if (unique.size() != ids.size()) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_SEND_INVALID);
        }
        List<ImUserStickerDO> owned = userStickerMapper.selectList(
                Wrappers.<ImUserStickerDO>lambdaQuery()
                        .eq(ImUserStickerDO::getTenantId, loginUser.getTenantId())
                        .eq(ImUserStickerDO::getUserId, loginUser.getUserId())
                        .in(ImUserStickerDO::getId, ids));
        if (owned.size() != ids.size()) {
            throw new ServiceException(ErrorCodeConstants.STICKER_NOT_FOUND);
        }
        OffsetDateTime now = OffsetDateTime.now();
        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            userStickerMapper.update(null, Wrappers.<ImUserStickerDO>lambdaUpdate()
                    .eq(ImUserStickerDO::getId, id)
                    .eq(ImUserStickerDO::getTenantId, loginUser.getTenantId())
                    .eq(ImUserStickerDO::getUserId, loginUser.getUserId())
                    .set(ImUserStickerDO::getSort, i)
                    .set(ImUserStickerDO::getUpdater, loginUser.getUserId())
                    .set(ImUserStickerDO::getUpdateTime, now));
        }
    }

    @Transactional
    public void deleteBatch(DeleteUserStickersReqVO request) {
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        List<Long> ids = request.getIds();
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        userStickerMapper.delete(Wrappers.<ImUserStickerDO>lambdaQuery()
                .eq(ImUserStickerDO::getTenantId, loginUser.getTenantId())
                .eq(ImUserStickerDO::getUserId, loginUser.getUserId())
                .in(ImUserStickerDO::getId, ids));
    }

    public ImUserStickerDO requireOwned(Long userStickerId) {
        if (userStickerId == null) {
            throw new ServiceException(ErrorCodeConstants.STICKER_NOT_FOUND);
        }
        LoginUser loginUser = SecurityFrameworkUtils.requireLoginUser();
        ImUserStickerDO row = userStickerMapper.selectById(userStickerId);
        if (row == null
                || !Objects.equals(row.getTenantId(), loginUser.getTenantId())
                || !Objects.equals(row.getUserId(), loginUser.getUserId())) {
            throw new ServiceException(ErrorCodeConstants.STICKER_NOT_FOUND);
        }
        return row;
    }

    private UserStickerItemRespVO upsertOwned(LoginUser loginUser, Long fileId, String name, Long sourceMessageId) {
        ImUserStickerDO existing = userStickerMapper.selectOne(
                Wrappers.<ImUserStickerDO>lambdaQuery()
                        .eq(ImUserStickerDO::getTenantId, loginUser.getTenantId())
                        .eq(ImUserStickerDO::getUserId, loginUser.getUserId())
                        .eq(ImUserStickerDO::getFileId, fileId));
        if (existing != null) {
            return toItem(existing);
        }
        long count = userStickerMapper.selectCount(
                Wrappers.<ImUserStickerDO>lambdaQuery()
                        .eq(ImUserStickerDO::getTenantId, loginUser.getTenantId())
                        .eq(ImUserStickerDO::getUserId, loginUser.getUserId()));
        if (count >= MAX_PER_USER) {
            throw new ServiceException(ErrorCodeConstants.STICKER_LIMIT_EXCEEDED);
        }
        OffsetDateTime now = OffsetDateTime.now();
        ImUserStickerDO row = new ImUserStickerDO();
        row.setTenantId(loginUser.getTenantId());
        row.setUserId(loginUser.getUserId());
        row.setFileId(fileId);
        row.setName(StringUtils.hasText(name) ? name.trim() : null);
        row.setSort((int) count);
        row.setSourceMessageId(sourceMessageId);
        row.setCreator(loginUser.getUserId());
        row.setCreateTime(now);
        row.setUpdater(loginUser.getUserId());
        row.setUpdateTime(now);
        userStickerMapper.insert(row);
        var binding = new com.relayflow.module.infra.api.file.dto.FileBindReqDTO();
        binding.setFileId(fileId); binding.setBizId(row.getId()); binding.setBizType("im_user_sticker");
        fileApi.bindFile(binding);
        return toItem(row);
    }

    private void requireImageFile(Long fileId) {
        FileRespDTO file;
        try {
            file = fileApi.requireReadable(SecurityFrameworkUtils.requireLoginTenantId(), SecurityFrameworkUtils.requireLoginUserId(), fileId);
        } catch (ServiceException ex) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_FILE_NOT_FOUND);
        }
        if (file == null || file.getId() == null) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_FILE_NOT_FOUND);
        }
        String mime = file.getMimeType();
        if (!StringUtils.hasText(mime) || !mime.toLowerCase().startsWith("image/")) {
            throw new ServiceException(ErrorCodeConstants.STICKER_FILE_TYPE_INVALID);
        }
        if (file.getSize() != null && file.getSize() > MAX_FILE_BYTES) {
            throw new ServiceException(ErrorCodeConstants.STICKER_FILE_TYPE_INVALID);
        }
    }

    private UserStickerItemRespVO toItem(ImUserStickerDO row) {
        UserStickerItemRespVO item = new UserStickerItemRespVO();
        item.setId(row.getId());
        item.setFileId(row.getFileId());
        item.setName(row.getName());
        item.setSort(row.getSort());
        item.setDownloadUrl(APP_FILE_DOWNLOAD_PREFIX + row.getFileId());
        return item;
    }
}
