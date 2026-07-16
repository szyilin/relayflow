package com.relayflow.module.im.controller.app.vo;

import com.relayflow.module.im.controller.app.vo.card.CardActionItemVO;
import com.relayflow.module.im.controller.app.vo.card.CardFieldVO;
import com.relayflow.module.im.controller.app.vo.card.CardHeaderVO;
import com.relayflow.module.im.controller.app.vo.card.CardMetaVO;
import lombok.Data;

import java.util.List;

@Data
public class ContentBlockVO {

    private String type;
    private String text;
    private String fileId;
    private String filename;
    private String mimeType;
    private Long size;
    private String downloadUrl;

    /** Deep-link metadata for {@code type=deeplink} (Bot business reach). */
    private String route;
    private String entityType;
    private String entityId;

    /** Present when {@code type=card} ({@code generic.v1}). */
    private Integer schema;
    private String cardId;
    private String template;
    private CardHeaderVO header;
    private List<CardFieldVO> fields;
    private List<CardActionItemVO> actions;
    private CardMetaVO meta;

    /** Present when {@code type=mention} (group @Bot / @user). */
    private String subjectType;
    /** Bot or user id when {@code type=mention}. */
    private Long subjectId;
    /** Bot code when mentioning a bot (preferred lookup key with subjectId). */
    private String botCode;
}
