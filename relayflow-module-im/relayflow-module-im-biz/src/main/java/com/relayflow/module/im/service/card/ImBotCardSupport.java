package com.relayflow.module.im.service.card;

import com.relayflow.common.exception.ServiceException;
import com.relayflow.module.im.api.bot.dto.card.ImBotCardDocument;
import com.relayflow.module.im.controller.app.vo.ContentBlockVO;
import com.relayflow.module.im.controller.app.vo.card.CardActionItemVO;
import com.relayflow.module.im.controller.app.vo.card.CardBehaviorVO;
import com.relayflow.module.im.controller.app.vo.card.CardFieldVO;
import com.relayflow.module.im.controller.app.vo.card.CardFormFieldVO;
import com.relayflow.module.im.controller.app.vo.card.CardHeaderVO;
import com.relayflow.module.im.controller.app.vo.card.CardMetaVO;
import com.relayflow.module.im.controller.app.vo.card.CardSourceVO;
import com.relayflow.module.im.enums.ErrorCodeConstants;
import com.relayflow.module.im.service.message.ImContentHelper;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Validates and maps {@link ImBotCardDocument} ↔ content block.
 */
public final class ImBotCardSupport {

    public static final String TEMPLATE_GENERIC_V1 = "generic.v1";
    public static final String BEHAVIOR_OPEN_URL = "open_url";
    public static final String BEHAVIOR_CALLBACK = "callback";

    private ImBotCardSupport() {
    }

    public static void validate(ImBotCardDocument card) {
        if (card == null) {
            throw new ServiceException(ErrorCodeConstants.CARD_CONTENT_INVALID);
        }
        if (!TEMPLATE_GENERIC_V1.equals(card.getTemplate())) {
            throw new ServiceException(ErrorCodeConstants.CARD_CONTENT_INVALID);
        }
        if (card.getHeader() == null || !StringUtils.hasText(card.getHeader().getTitle())) {
            throw new ServiceException(ErrorCodeConstants.CARD_CONTENT_INVALID);
        }
        if (CollectionUtils.isEmpty(card.getActions())) {
            throw new ServiceException(ErrorCodeConstants.CARD_CONTENT_INVALID);
        }
        for (ImBotCardDocument.ImBotCardAction action : card.getActions()) {
            if (action == null || !StringUtils.hasText(action.getId()) || !StringUtils.hasText(action.getLabel())) {
                throw new ServiceException(ErrorCodeConstants.CARD_CONTENT_INVALID);
            }
            ImBotCardDocument.ImBotCardBehavior behavior = action.getBehavior();
            if (behavior == null || !StringUtils.hasText(behavior.getType())) {
                throw new ServiceException(ErrorCodeConstants.CARD_CONTENT_INVALID);
            }
            String type = behavior.getType().trim();
            if (BEHAVIOR_OPEN_URL.equals(type)) {
                if (!StringUtils.hasText(behavior.getRoute())) {
                    throw new ServiceException(ErrorCodeConstants.CARD_CONTENT_INVALID);
                }
            } else if (BEHAVIOR_CALLBACK.equals(type)) {
                if (!StringUtils.hasText(behavior.getActionKey())) {
                    throw new ServiceException(ErrorCodeConstants.CARD_CONTENT_INVALID);
                }
            } else {
                throw new ServiceException(ErrorCodeConstants.CARD_CONTENT_INVALID);
            }
        }
    }

    public static ContentBlockVO toContentBlock(ImBotCardDocument card) {
        validate(card);
        ContentBlockVO block = new ContentBlockVO();
        block.setType(ImContentHelper.BLOCK_TYPE_CARD);
        block.setSchema(card.getSchema() != null ? card.getSchema() : 1);
        block.setCardId(StringUtils.hasText(card.getCardId()) ? card.getCardId().trim() : "c_" + UUID.randomUUID());
        block.setTemplate(card.getTemplate().trim());

        CardHeaderVO header = new CardHeaderVO();
        header.setTitle(card.getHeader().getTitle().trim());
        if (StringUtils.hasText(card.getHeader().getSubtitle())) {
            header.setSubtitle(card.getHeader().getSubtitle().trim());
        }
        block.setHeader(header);

        if (!CollectionUtils.isEmpty(card.getFields())) {
            List<CardFieldVO> fields = new ArrayList<>();
            for (ImBotCardDocument.ImBotCardField field : card.getFields()) {
                if (field == null) {
                    continue;
                }
                CardFieldVO item = new CardFieldVO();
                item.setLabel(field.getLabel());
                item.setValue(field.getValue());
                fields.add(item);
            }
            block.setFields(fields);
        }

        List<CardActionItemVO> actions = new ArrayList<>();
        for (ImBotCardDocument.ImBotCardAction action : card.getActions()) {
            CardActionItemVO item = new CardActionItemVO();
            item.setId(action.getId().trim());
            item.setLabel(action.getLabel().trim());
            item.setStyle(StringUtils.hasText(action.getStyle()) ? action.getStyle().trim() : "default");
            CardBehaviorVO behavior = new CardBehaviorVO();
            behavior.setType(action.getBehavior().getType().trim());
            behavior.setRoute(action.getBehavior().getRoute());
            behavior.setActionKey(action.getBehavior().getActionKey());
            behavior.setPayload(action.getBehavior().getPayload());
            if (!CollectionUtils.isEmpty(action.getBehavior().getForm())) {
                List<CardFormFieldVO> form = new ArrayList<>();
                for (ImBotCardDocument.ImBotCardFormField formField : action.getBehavior().getForm()) {
                    if (formField == null) {
                        continue;
                    }
                    CardFormFieldVO mapped = new CardFormFieldVO();
                    mapped.setName(formField.getName());
                    mapped.setLabel(formField.getLabel());
                    mapped.setRequired(formField.getRequired());
                    mapped.setControl(formField.getControl());
                    form.add(mapped);
                }
                behavior.setForm(form);
            }
            item.setBehavior(behavior);
            actions.add(item);
        }
        block.setActions(actions);

        if (card.getMeta() != null) {
            CardMetaVO meta = new CardMetaVO();
            meta.setExpiresAt(card.getMeta().getExpiresAt());
            if (card.getMeta().getSource() != null) {
                CardSourceVO source = new CardSourceVO();
                source.setDomain(card.getMeta().getSource().getDomain());
                source.setEntityType(card.getMeta().getSource().getEntityType());
                source.setEntityId(card.getMeta().getSource().getEntityId());
                meta.setSource(source);
            }
            block.setMeta(meta);
        }
        return block;
    }

    @SuppressWarnings("unchecked")
    public static ContentBlockVO fromMap(Map<String, Object> cardMap) {
        if (cardMap == null || cardMap.isEmpty()) {
            throw new ServiceException(ErrorCodeConstants.CARD_CONTENT_INVALID);
        }
        ContentBlockVO block = new ContentBlockVO();
        block.setType(ImContentHelper.BLOCK_TYPE_CARD);
        Object schema = cardMap.get("schema");
        if (schema instanceof Number number) {
            block.setSchema(number.intValue());
        } else {
            block.setSchema(1);
        }
        block.setCardId(asString(cardMap.get("cardId")));
        block.setTemplate(asString(cardMap.get("template")));
        if (!StringUtils.hasText(block.getTemplate())) {
            block.setTemplate(TEMPLATE_GENERIC_V1);
        }

        Object headerObj = cardMap.get("header");
        if (headerObj instanceof Map<?, ?> headerMap) {
            CardHeaderVO header = new CardHeaderVO();
            header.setTitle(asString(headerMap.get("title")));
            header.setSubtitle(asString(headerMap.get("subtitle")));
            block.setHeader(header);
        }

        Object fieldsObj = cardMap.get("fields");
        if (fieldsObj instanceof List<?> fieldList) {
            List<CardFieldVO> fields = new ArrayList<>();
            for (Object item : fieldList) {
                if (!(item instanceof Map<?, ?> fieldMap)) {
                    continue;
                }
                CardFieldVO field = new CardFieldVO();
                field.setLabel(asString(fieldMap.get("label")));
                field.setValue(asString(fieldMap.get("value")));
                fields.add(field);
            }
            block.setFields(fields);
        }

        Object actionsObj = cardMap.get("actions");
        if (actionsObj instanceof List<?> actionList) {
            List<CardActionItemVO> actions = new ArrayList<>();
            for (Object item : actionList) {
                if (!(item instanceof Map<?, ?> actionMap)) {
                    continue;
                }
                CardActionItemVO action = new CardActionItemVO();
                action.setId(asString(actionMap.get("id")));
                action.setLabel(asString(actionMap.get("label")));
                action.setStyle(asString(actionMap.get("style")));
                Object behaviorObj = actionMap.get("behavior");
                if (behaviorObj instanceof Map<?, ?> behaviorMap) {
                    CardBehaviorVO behavior = new CardBehaviorVO();
                    behavior.setType(asString(behaviorMap.get("type")));
                    behavior.setRoute(asString(behaviorMap.get("route")));
                    behavior.setActionKey(asString(behaviorMap.get("actionKey")));
                    Object payload = behaviorMap.get("payload");
                    if (payload instanceof Map<?, ?> payloadMap) {
                        behavior.setPayload((Map<String, Object>) payloadMap);
                    }
                    Object formObj = behaviorMap.get("form");
                    if (formObj instanceof List<?> formList) {
                        List<CardFormFieldVO> form = new ArrayList<>();
                        for (Object formItem : formList) {
                            if (!(formItem instanceof Map<?, ?> formMap)) {
                                continue;
                            }
                            CardFormFieldVO formField = new CardFormFieldVO();
                            formField.setName(asString(formMap.get("name")));
                            formField.setLabel(asString(formMap.get("label")));
                            Object required = formMap.get("required");
                            if (required instanceof Boolean bool) {
                                formField.setRequired(bool);
                            }
                            formField.setControl(asString(formMap.get("control")));
                            form.add(formField);
                        }
                        behavior.setForm(form);
                    }
                    action.setBehavior(behavior);
                }
                actions.add(action);
            }
            block.setActions(actions);
        }

        Object metaObj = cardMap.get("meta");
        if (metaObj instanceof Map<?, ?> metaMap) {
            CardMetaVO meta = new CardMetaVO();
            meta.setExpiresAt(asString(metaMap.get("expiresAt")));
            Object sourceObj = metaMap.get("source");
            if (sourceObj instanceof Map<?, ?> sourceMap) {
                CardSourceVO source = new CardSourceVO();
                source.setDomain(asString(sourceMap.get("domain")));
                source.setEntityType(asString(sourceMap.get("entityType")));
                source.setEntityId(asString(sourceMap.get("entityId")));
                meta.setSource(source);
            }
            block.setMeta(meta);
        }
        return block;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
