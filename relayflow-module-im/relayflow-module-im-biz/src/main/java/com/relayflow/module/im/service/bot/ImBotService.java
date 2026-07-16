package com.relayflow.module.im.service.bot;

import com.relayflow.module.im.api.bot.dto.ImBotSendCommand;
import com.relayflow.module.im.api.bot.dto.ImBotSendResult;

public interface ImBotService {

    ImBotSendResult send(ImBotSendCommand command);

    void ensureUserEnablementsOnActive(Long tenantId, Long userId);
}
