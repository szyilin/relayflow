package com.relayflow.module.im.service.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.im.controller.app.vo.ContentBlockVO;
import com.relayflow.module.im.controller.app.vo.MessageContentVO;
import com.relayflow.module.im.enums.ErrorCodeConstants;
import com.relayflow.module.infra.api.file.FileApi;
import com.relayflow.module.infra.api.file.dto.FileRespDTO;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
public class ImContentHelper {

    public static final String MESSAGE_TYPE_TEXT = "text";
    public static final String MESSAGE_TYPE_IMAGE = "image";
    public static final String MESSAGE_TYPE_FILE = "file";
    public static final String BLOCK_TYPE_FILE = "file";

    private static final String APP_FILE_DOWNLOAD_PREFIX = "/app-api/infra/file/download?fileId=";

    private final ObjectMapper objectMapper;
    private final FileApi fileApi;

    public ImContentHelper(ObjectMapper objectMapper, FileApi fileApi) {
        this.objectMapper = objectMapper;
        this.fileApi = fileApi;
    }

    public void validateUserMessage(String type, MessageContentVO content) {
        String messageType = normalizeMessageType(type);
        if (MESSAGE_TYPE_TEXT.equals(messageType)) {
            validateTextContent(content);
            return;
        }
        validateFileContent(messageType, content);
    }

    public String normalizeMessageType(String type) {
        if (!StringUtils.hasText(type) || MESSAGE_TYPE_TEXT.equals(type)) {
            return MESSAGE_TYPE_TEXT;
        }
        if (MESSAGE_TYPE_IMAGE.equals(type) || MESSAGE_TYPE_FILE.equals(type)) {
            return type;
        }
        throw new ServiceException(ErrorCodeConstants.MESSAGE_SEND_INVALID);
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

    public void validateFileContent(String messageType, MessageContentVO content) {
        if (content == null || content.getVersion() == null || content.getVersion() != 1) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_CONTENT_INVALID);
        }
        if (CollectionUtils.isEmpty(content.getBlocks())) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_CONTENT_INVALID);
        }

        ContentBlockVO fileBlock = content.getBlocks().stream()
                .filter(block -> BLOCK_TYPE_FILE.equals(block.getType()))
                .findFirst()
                .orElse(null);
        if (fileBlock == null || !StringUtils.hasText(fileBlock.getFileId())) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_CONTENT_INVALID);
        }

        Long fileId = parseFileId(fileBlock.getFileId());
        FileRespDTO file = requireAccessibleFile(fileId);

        String mimeType = StringUtils.hasText(fileBlock.getMimeType())
                ? fileBlock.getMimeType()
                : file.getMimeType();
        if (MESSAGE_TYPE_IMAGE.equals(messageType)) {
            if (!StringUtils.hasText(mimeType) || !mimeType.toLowerCase().startsWith("image/")) {
                throw new ServiceException(ErrorCodeConstants.MESSAGE_FILE_TYPE_INVALID);
            }
        }

        if (!StringUtils.hasText(fileBlock.getFilename())) {
            fileBlock.setFilename(file.getOriginalName());
        }
        if (!StringUtils.hasText(fileBlock.getMimeType())) {
            fileBlock.setMimeType(file.getMimeType());
        }
        if (fileBlock.getSize() == null && file.getSize() != null) {
            fileBlock.setSize(file.getSize());
        }
    }

    public void enrichDownloadUrls(MessageContentVO content) {
        if (content == null || CollectionUtils.isEmpty(content.getBlocks())) {
            return;
        }
        for (ContentBlockVO block : content.getBlocks()) {
            if (!BLOCK_TYPE_FILE.equals(block.getType()) || !StringUtils.hasText(block.getFileId())) {
                continue;
            }
            block.setDownloadUrl(APP_FILE_DOWNLOAD_PREFIX + block.getFileId().trim());
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

    public String buildPreview(String messageType, MessageContentVO content) {
        if (MESSAGE_TYPE_IMAGE.equals(messageType)) {
            return "[图片]";
        }
        if (MESSAGE_TYPE_FILE.equals(messageType)) {
            ContentBlockVO fileBlock = firstFileBlock(content);
            if (fileBlock != null && StringUtils.hasText(fileBlock.getFilename())) {
                return "[文件] " + fileBlock.getFilename().trim();
            }
            return "[文件]";
        }
        return buildTextPreview(content);
    }

    public String buildTextPreview(MessageContentVO content) {
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

    private FileRespDTO requireAccessibleFile(Long fileId) {
        try {
            FileRespDTO file = fileApi.getFile(fileId);
            if (file == null || file.getId() == null) {
                throw new ServiceException(ErrorCodeConstants.MESSAGE_FILE_NOT_FOUND);
            }
            return file;
        } catch (ServiceException ex) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_FILE_NOT_FOUND);
        }
    }

    private Long parseFileId(String fileId) {
        try {
            return Long.parseLong(fileId.trim());
        } catch (NumberFormatException ex) {
            throw new ServiceException(ErrorCodeConstants.MESSAGE_FILE_NOT_FOUND);
        }
    }

    private ContentBlockVO firstFileBlock(MessageContentVO content) {
        if (content == null || CollectionUtils.isEmpty(content.getBlocks())) {
            return null;
        }
        return content.getBlocks().stream()
                .filter(block -> BLOCK_TYPE_FILE.equals(block.getType()))
                .findFirst()
                .orElse(null);
    }
}
