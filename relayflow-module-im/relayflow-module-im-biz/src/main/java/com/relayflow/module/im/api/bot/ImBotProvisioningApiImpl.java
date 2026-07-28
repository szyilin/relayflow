package com.relayflow.module.im.api.bot;

import com.relayflow.module.im.api.bot.dto.ImBotProvisionAgentCarrierCommand;
import com.relayflow.module.im.api.bot.dto.ImBotProvisionResult;
import com.relayflow.module.im.api.bot.dto.ImBotRetireAgentCarrierCommand;
import com.relayflow.module.im.api.bot.dto.ImBotUpdateAgentCarrierProfileCommand;
import com.relayflow.module.im.service.bot.ImBotProvisioningServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ImBotProvisioningApiImpl implements ImBotProvisioningApi {

    private final ImBotProvisioningServiceImpl provisioningService;

    @Override
    public ImBotProvisionResult provisionAgentCarrier(ImBotProvisionAgentCarrierCommand command) {
        return provisioningService.provisionAgentCarrier(command);
    }

    @Override
    public void retireAgentCarrier(ImBotRetireAgentCarrierCommand command) {
        provisioningService.retireAgentCarrier(command);
    }

    @Override
    public ImBotProvisionResult updateAgentCarrierProfile(ImBotUpdateAgentCarrierProfileCommand command) {
        return provisioningService.updateAgentCarrierProfile(command);
    }
}
