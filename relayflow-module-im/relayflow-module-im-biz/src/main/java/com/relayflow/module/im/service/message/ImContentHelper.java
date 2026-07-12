package com.relayflow.module.im.service.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.im.controller.app.vo.ContentBlockVO;
import com.relayflow.module.im.controller.app.vo.MessageContentVO;
import com.relayflow.module.im.enums.ErrorCodeConstants;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
public class ImContentHelper {

    private final ObjectMapper objectMapper;

    public ImContentHelper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void validateTextContent(MessageContentVO content) {
        if (content == null || content.getVersion() == null || content.getVersion() != 1) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_CONTENT_INVALID);
        }
        if (CollectionUtils.isEmpty(content.getBlocks())) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_CONTENT_INVALID);
        }
        boolean hasText = content.getBlocks().stream()
                .anyMatch(block -> "text".equals(block.getType()) && StringUtils.hasText(block.getText()));
        if (!hasText) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_CONTENT_INVALID);
        }
    }

    public String toJson(MessageContentVO content) {
        try {
            return objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException ex) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_CONTENT_INVALID);
        }
    }

    public MessageContentVO fromJson(String contentJson) {
        if (!StringUtils.hasText(contentJson)) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_CONTENT_INVALID);
        }
        try {
            return objectMapper.readValue(contentJson, MessageContentVO.class);
        } catch (JsonProcessingException ex) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_CONTENT_INVALID);
        }
    }

    public String buildPreview(MessageContentVO content) {
        if (content == null || CollectionUtils.isEmpty(content.getBlocks())) {
            return "";
        }
        StringBuilder preview = new StringBuilder();
        for (ContentBlockVO block : content.getBlocks()) {
            if ("text".equals(block.getType()) && StringUtils.hasText(block.getText())) {
                if (!preview.isEmpty()) {
                    preview.append(' ');
                }
                preview.append(block.getText().trim());
            }
        }
        String text = preview.toString();
        if (text.length() > 512) {
            return text.substring(0, 512);
        }
        return text;
    }

    public String firstAvatarChar(String displayName) {
        if (!StringUtils.hasText(displayName)) {
            return "?";
        }
        return displayName.substring(0, 1);
    }
}
