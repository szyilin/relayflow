package com.relayflow.module.rtc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Data
@Component
@ConfigurationProperties(prefix = "relayflow.rtc.livekit")
public class RtcLiveKitProperties {

    /** Browser-facing WebSocket URL, e.g. ws://127.0.0.1:7880 */
    private String url = "ws://127.0.0.1:7880";

    /**
     * Optional HTTP base for LiveKit Room Service (defaults from {@link #url}: ws→http, wss→https).
     */
    private String apiUrl = "";

    private String apiKey = "devkey";

    private String apiSecret = "secret";

    /** Shared secret for webhook Authorization: Bearer … (defaults to apiSecret if blank) */
    private String webhookSecret = "";

    /** Default join token TTL in seconds */
    private long tokenTtlSeconds = 7200;

    /** When true, endSession calls LiveKit DeleteRoom. */
    private boolean roomServiceEnabled = true;

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

    public String resolveApiUrl() {
        if (StringUtils.hasText(apiUrl)) {
            String trimmed = apiUrl.trim();
            return trimmed.endsWith("/") ? trimmed : trimmed + "/";
        }
        if (!StringUtils.hasText(url)) {
            return "";
        }
        String u = url.trim();
        if (u.startsWith("wss://")) {
            u = "https://" + u.substring("wss://".length());
        } else if (u.startsWith("ws://")) {
            u = "http://" + u.substring("ws://".length());
        }
        return u.endsWith("/") ? u : u + "/";
    }
}
