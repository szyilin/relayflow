package com.relayflow.module.im.service.message;

import com.relayflow.module.im.controller.app.vo.ForwardMessageReqVO;
import com.relayflow.module.im.controller.app.vo.MessageItemRespVO;
import com.relayflow.module.im.controller.app.vo.SendMessageReqVO;
import com.relayflow.module.im.controller.app.vo.SendMessageRespVO;
import com.relayflow.module.im.service.message.dto.RealtimeSendContext;

import java.util.List;

public interface ImMessageService {

    List<MessageItemRespVO> listMessages(Long conversationId, Long afterSeq);

    SendMessageRespVO sendMessage(Long tenantId, Long userId, SendMessageReqVO request, RealtimeSendContext realtimeContext);

    SendMessageRespVO sendMyMessage(SendMessageReqVO request);

    MessageItemRespVO forwardMessage(ForwardMessageReqVO request);

    void recallMessage(Long messageId);

    void editMessage(Long messageId, String content);

    /**
     * Persist an in-conversation <em>environment</em> tip ({@code sender_type=system}),
     * e.g. "X joined the group". Must not be used for cross-module business reach —
     * use {@link com.relayflow.module.im.api.bot.ImBotApi#send} instead.
     */
    void sendSystemMessage(Long tenantId, Long conversationId, String text);

    /**
     * Persist a Bot Runtime reply in an existing conversation ({@code sender_type=bot})
     * and push {@code message.new} to User members only.
     *
     * @param mentionUserId when non-null, platform prepends a structured user {@code mention} block
     *                      (group reply @trigger); agents MUST NOT decide this
     */
    void sendBotReply(Long tenantId, Long conversationId, Long botId, String botName, String text,
                      Long mentionUserId);
}
