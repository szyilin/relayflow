package com.relayflow.module.im.service.bot.event;

import com.relayflow.framework.messaging.DomainEvent;
import com.relayflow.framework.messaging.DomainEventHandler;
import com.relayflow.module.im.api.bot.ImBotApi;
import com.relayflow.module.im.api.bot.dto.ImBotSendCommand;
import com.relayflow.module.im.api.bot.dto.ImBotSendTarget;
import com.relayflow.module.im.api.bot.dto.card.ImBotCardDocument;
import com.relayflow.module.system.api.event.MemberInvitedPayload;
import com.relayflow.module.system.api.event.SystemDomainEventTypes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberInvitedImListener implements DomainEventHandler<MemberInvitedPayload> {

    private static final String ORG_ASSISTANT_BOT_CODE = "org-assistant";
    private static final String MEMBER_INVITE_DEDUPE_PREFIX = "MEMBER_INVITE:";
    private static final String ACTION_KEY_ACCEPT = "system.member.invite.accept";

    private final ImBotApi imBotApi;

    @Override
    public String eventType() {
        return SystemDomainEventTypes.MEMBER_INVITED;
    }

    @Override
    public Class<MemberInvitedPayload> payloadType() {
        return MemberInvitedPayload.class;
    }

    @Override
    public void handle(DomainEvent envelope, MemberInvitedPayload payload) {
        if (payload == null || payload.getInvitingTenantId() == null || payload.getInviteeUserId() == null) {
            log.warn("Ignore member.invited with incomplete payload: eventId={}",
                    envelope != null ? envelope.getEventId() : null);
            return;
        }
        try {
            ImBotSendTarget target = new ImBotSendTarget();
            target.setScope(ImBotSendTarget.SCOPE_ALL_ACTIVE_MEMBERSHIPS);
            target.setUserId(payload.getInviteeUserId());

            ImBotSendCommand command = new ImBotSendCommand();
            command.setBotCode(ORG_ASSISTANT_BOT_CODE);
            command.setCard(buildPendingCard(payload));
            command.setDedupeKey(MEMBER_INVITE_DEDUPE_PREFIX + payload.getInvitingTenantId());
            command.setEntityType("tenant");
            command.setEntityId(String.valueOf(payload.getInvitingTenantId()));
            command.setTarget(target);
            imBotApi.send(command);
        } catch (Exception ex) {
            log.warn("Invite bot message failed: invitingTenantId={}, inviteeUserId={}",
                    payload.getInvitingTenantId(), payload.getInviteeUserId(), ex);
        }
    }

    private static ImBotCardDocument buildPendingCard(MemberInvitedPayload payload) {
        String safeTenantName = payload.getTenantName() != null ? payload.getTenantName() : "企业";
        String safeInviter = payload.getInviterNickname() != null ? payload.getInviterNickname() : "管理员";
        Long invitingTenantId = payload.getInvitingTenantId();

        ImBotCardDocument card = new ImBotCardDocument();
        card.setSchema(1);
        card.setCardId("c_invite_" + invitingTenantId);
        card.setTemplate("generic.v1");

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
}
