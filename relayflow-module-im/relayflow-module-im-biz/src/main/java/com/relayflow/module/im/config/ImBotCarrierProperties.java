package com.relayflow.module.im.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "relayflow.im.bot-carrier")
public class ImBotCarrierProperties {

    /** Recent messages injected for bot_dm (excluding current trigger). */
    private int dmHistoryLimit = 20;

    /** Hard cap for dm history. */
    private int dmHistoryHardCap = 50;

    /** Nearby window half-size for group @Bot (before/after). */
    private int groupNearbyWindow = 5;

    /** Hard cap for nearby each side. */
    private int nearbyHardCap = 20;

    /** Max characters across injected history texts. */
    private int historyMaxChars = 8000;

    /** Preview length for reply_to summaries. */
    private int replyPreviewMaxChars = 80;
}
