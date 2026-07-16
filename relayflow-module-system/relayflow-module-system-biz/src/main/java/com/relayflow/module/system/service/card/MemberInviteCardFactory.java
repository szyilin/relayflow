package com.relayflow.module.system.service.card;

import com.relayflow.module.im.api.bot.dto.card.ImBotCardDocument;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * Builds org-assistant invite cards ({@code generic.v1}).
 */
public final class MemberInviteCardFactory {

    public static final String ACTION_KEY_ACCEPT = "system.member.invite.accept";
    public static final String TEMPLATE_GENERIC_V1 = "generic.v1";

    private MemberInviteCardFactory() {
    }

    public static ImBotCardDocument pending(Long invitingTenantId, String tenantName, String inviterNickname) {
        String safeTenantName = tenantName != null ? tenantName : "企业";
        String safeInviter = inviterNickname != null ? inviterNickname : "管理员";

        ImBotCardDocument card = new ImBotCardDocument();
        card.setSchema(1);
        card.setCardId("c_invite_" + invitingTenantId);
        card.setTemplate(TEMPLATE_GENERIC_V1);

        ImBotCardDocument.ImBotCardHeader header = new ImBotCardDocument.ImBotCardHeader();
        header.setTitle("邀请加入企业");
        header.setSubtitle(safeTenantName);
        card.setHeader(header);

        ImBotCardDocument.ImBotCardField inviterField = new ImBotCardDocument.ImBotCardField();
        inviterField.setLabel("邀请人");
        inviterField.setValue(safeInviter);
        ImBotCardDocument.ImBotCardField tenantField = new ImBotCardDocument.ImBotCardField();
        tenantField.setLabel("企业");
        tenantField.setValue(safeTenantName);
        card.setFields(List.of(inviterField, tenantField));

        ImBotCardDocument.ImBotCardAction accept = new ImBotCardDocument.ImBotCardAction();
        accept.setId("accept");
        accept.setLabel("接受邀请");
        accept.setStyle("primary");
        ImBotCardDocument.ImBotCardBehavior behavior = new ImBotCardDocument.ImBotCardBehavior();
        behavior.setType("callback");
        behavior.setActionKey(ACTION_KEY_ACCEPT);
        behavior.setPayload(Map.of(
                "tenantId", String.valueOf(invitingTenantId),
                "tenantName", safeTenantName));
        accept.setBehavior(behavior);
        card.setActions(List.of(accept));

        ImBotCardDocument.ImBotCardMeta meta = new ImBotCardDocument.ImBotCardMeta();
        meta.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(30).toString());
        ImBotCardDocument.ImBotCardSource source = new ImBotCardDocument.ImBotCardSource();
        source.setDomain("system");
        source.setEntityType("tenant_invite");
        source.setEntityId(String.valueOf(invitingTenantId));
        meta.setSource(source);
        card.setMeta(meta);
        return card;
    }

    public static Map<String, Object> accepted(String tenantName) {
        String safeTenantName = tenantName != null ? tenantName : "企业";
        return Map.of(
                "schema", 1,
                "cardId", "c_invite_done",
                "template", TEMPLATE_GENERIC_V1,
                "header", Map.of(
                        "title", "已加入企业",
                        "subtitle", safeTenantName),
                "fields", List.of(
                        Map.of("label", "企业", "value", safeTenantName),
                        Map.of("label", "状态", "value", "已接受")),
                "actions", List.of(),
                "meta", Map.of(
                        "source", Map.of(
                                "domain", "system",
                                "entityType", "tenant_invite",
                                "entityId", "done")));
    }
}
