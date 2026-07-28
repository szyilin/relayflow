package com.relayflow.module.im.service.bot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImBotProvisioningServiceImplTest {

    @Test
    void carrierCode_isStable() {
        assertEquals("agent-carrier-1001", ImBotProvisioningServiceImpl.carrierCode(1001L));
    }
}
