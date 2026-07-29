package com.relayflow.module.im.service.sticker;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.im.controller.app.vo.ContentBlockVO;
import com.relayflow.module.im.controller.app.vo.MessageContentVO;
import com.relayflow.module.im.controller.app.vo.StickerItemRespVO;
import com.relayflow.module.im.controller.app.vo.StickerPackRespVO;
import com.relayflow.module.im.dal.dataobject.ImStickerDO;
import com.relayflow.module.im.dal.dataobject.ImStickerPackDO;
import com.relayflow.module.im.dal.mapper.ImStickerMapper;
import com.relayflow.module.im.dal.mapper.ImStickerPackMapper;
import com.relayflow.module.im.enums.ErrorCodeConstants;
import com.relayflow.module.im.service.message.ImContentHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImStickerService {

    private static final String APP_FILE_DOWNLOAD_PREFIX = "/app-api/infra/file/download?fileId=";

    private final ImStickerPackMapper stickerPackMapper;
    private final ImStickerMapper stickerMapper;

    public List<StickerPackRespVO> listVisiblePacks() {
        List<ImStickerPackDO> packs = stickerPackMapper.selectList(
                Wrappers.<ImStickerPackDO>lambdaQuery()
                        .eq(ImStickerPackDO::getStatus, 1)
                        .orderByAsc(ImStickerPackDO::getSort)
                        .orderByAsc(ImStickerPackDO::getId));
        List<StickerPackRespVO> result = new ArrayList<>();
        for (ImStickerPackDO pack : packs) {
            StickerPackRespVO vo = new StickerPackRespVO();
            vo.setId(pack.getId());
            vo.setCode(pack.getCode());
            vo.setName(pack.getName());
            vo.setScope(pack.getScope());
            List<ImStickerDO> stickers = stickerMapper.selectList(
                    Wrappers.<ImStickerDO>lambdaQuery()
                            .eq(ImStickerDO::getPackId, pack.getId())
                            .orderByAsc(ImStickerDO::getSort)
                            .orderByAsc(ImStickerDO::getId));
            vo.setStickers(stickers.stream().map(this::toItem).toList());
            result.add(vo);
        }
        return result;
    }

    public ImStickerDO requireVisibleSticker(Long stickerId) {
        if (stickerId == null) {
            throw new ServiceException(ErrorCodeConstants.STICKER_NOT_FOUND);
        }
        ImStickerDO sticker = stickerMapper.selectById(stickerId);
        if (sticker == null) {
            throw new ServiceException(ErrorCodeConstants.STICKER_NOT_FOUND);
        }
        ImStickerPackDO pack = stickerPackMapper.selectById(sticker.getPackId());
        if (pack == null || pack.getStatus() == null || pack.getStatus() != 1) {
            throw new ServiceException(ErrorCodeConstants.STICKER_NOT_FOUND);
        }
        return sticker;
    }

    /**
     * Rewrites sticker send content with authoritative packId / fileId / emoji from catalog.
     */
    public MessageContentVO resolveSendContent(MessageContentVO content, ImContentHelper contentHelper) {
        ContentBlockVO input = contentHelper.firstStickerBlock(content);
        if (input == null || input.getStickerId() == null) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_CONTENT_INVALID);
        }
        ImStickerDO sticker = requireVisibleSticker(input.getStickerId());
        ContentBlockVO block = new ContentBlockVO();
        block.setType(ImContentHelper.BLOCK_TYPE_STICKER);
        block.setStickerId(sticker.getId());
        block.setPackId(sticker.getPackId());
        if (sticker.getFileId() != null) {
            block.setFileId(String.valueOf(sticker.getFileId()));
            block.setDownloadUrl(APP_FILE_DOWNLOAD_PREFIX + sticker.getFileId());
        }
        if (StringUtils.hasText(sticker.getEmoji())) {
            block.setEmoji(sticker.getEmoji());
            block.setText(sticker.getEmoji());
        }
        MessageContentVO resolved = new MessageContentVO();
        resolved.setVersion(1);
        resolved.setBlocks(List.of(block));
        return resolved;
    }

    private StickerItemRespVO toItem(ImStickerDO sticker) {
        StickerItemRespVO item = new StickerItemRespVO();
        item.setId(sticker.getId());
        item.setPackId(sticker.getPackId());
        item.setCode(sticker.getCode());
        item.setName(sticker.getName());
        item.setEmoji(sticker.getEmoji());
        if (sticker.getFileId() != null) {
            item.setDownloadUrl(APP_FILE_DOWNLOAD_PREFIX + sticker.getFileId());
        }
        return item;
    }
}
