package com.relayflow.module.im.service.message;

import com.relayflow.module.im.controller.app.vo.MessageItemRespVO;
import com.relayflow.module.im.controller.app.vo.SendMessageReqVO;
import com.relayflow.module.im.controller.app.vo.SendMessageRespVO;
import com.relayflow.module.im.service.message.dto.RealtimeSendContext;

import java.util.List;

public interface ImMessageService {

    List<MessageItemRespVO> listMessages(Long tenantId, Long userId, Long conversationId, Long afterSeq);

    SendMessageRespVO sendMessage(Long tenantId, Long userId, SendMessageReqVO request, RealtimeSendContext realtimeContext);

    void sendSystemMessage(Long tenantId, Long conversationId, String text);
}
