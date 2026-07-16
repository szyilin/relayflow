package com.relayflow.module.im.api.bot;

import com.relayflow.module.im.api.bot.dto.ImBotSendCommand;
import com.relayflow.module.im.api.bot.dto.ImBotSendResult;
import com.relayflow.module.im.service.bot.ImBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ImBotApiImpl implements ImBotApi {

    private final ImBotService imBotService;

    @Override
    public ImBotSendResult send(ImBotSendCommand command) {
        return imBotService.send(command);
    }

    @Override
    public void ensureUserEnablementsOnActive(Long tenantId, Long userId) {
        imBotService.ensureUserEnablementsOnActive(tenantId, userId);
    }
}
