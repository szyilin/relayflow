package com.relayflow.module.rtc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "relayflow.rtc.livekit")
public class RtcLiveKitProperties {

    /** Browser-facing WebSocket URL, e.g. ws://127.0.0.1:7880 */
    private String url = "ws://127.0.0.1:7880";

    private String apiKey = "devkey";

    private String apiSecret = "secret";

    /** Shared secret for webhook Authorization: Bearer … (defaults to apiSecret if blank) */
    private String webhookSecret = "";

    /** Default join token TTL in seconds */
    private long tokenTtlSeconds = 7200;

    public String resolveWebhookSecret() {
        if (webhookSecret != null && !webhookSecret.isBlank()) {
            return webhookSecret;
        }
        return apiSecret;
    }

    public boolean isConfigured() {
        return url != null && !url.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && apiSecret != null && !apiSecret.isBlank();
    }
}
