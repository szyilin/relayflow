package com.relayflow.module.docs.service.notify;

import com.relayflow.module.docs.dal.dataobject.DocObjectDO;
import com.relayflow.module.im.api.bot.ImBotApi;
import com.relayflow.module.im.api.bot.dto.ImBotSendCommand;
import com.relayflow.module.im.api.bot.dto.ImBotSendTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Docs reach via {@code docs-bot} + {@link ImBotApi} (best-effort).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocsBotNotifyService {

    private static final String BOT_CODE = "docs-bot";

    private final ImBotApi imBotApi;

    public void notifyShared(DocObjectDO object, Long granteeUserId, String permission) {
        if (object == null || granteeUserId == null) {
            return;
        }
        String title = object.getTitle() != null ? object.getTitle() : "未命名文档";
        String permLabel = "EDIT".equals(permission) ? "可编辑" : "可阅读";
        String text = "向你分享了文档「" + title + "」（" + permLabel + "）";

        ImBotSendTarget target = new ImBotSendTarget();
        target.setScope(ImBotSendTarget.SCOPE_SINGLE);
        target.setTenantId(object.getTenantId());
        target.setUserId(granteeUserId);

        ImBotSendCommand command = new ImBotSendCommand();
        command.setBotCode(BOT_CODE);
        command.setText(text);
        command.setDedupeKey("ds:" + object.getId() + ":" + granteeUserId + ":" + permission);
        command.setRoute("/app/docs?docId=" + object.getId());
        command.setEntityType("doc");
        command.setEntityId(String.valueOf(object.getId()));
        command.setTarget(target);

        try {
            imBotApi.send(command);
        } catch (Exception ex) {
            log.warn("docs-bot share notify failed (best-effort): objectId={}, grantee={}",
                    object.getId(), granteeUserId, ex);
        }
    }
}
