package com.relayflow.framework.websocket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "relayflow.websocket")
public class WebSocketProperties {

    /** 是否启用 WebSocket 端点 */
    private boolean enable = true;

    /** WebSocket 路径，默认 /infra/ws */
    private String path = "/infra/ws";

    /** 发送模式：local（单实例）| redis（多实例 Pub/Sub） */
    private String senderType = "local";

    /** 允许的 Origin，逗号分隔或单个 * */
    private String allowedOrigins = "*";

    /** Redis 在线状态 key TTL（秒） */
    private int heartbeatTtlSeconds = 60;

    /** 握手 query 参数名 */
    private String tokenQueryParam = "token";

    public boolean isRedisSender() {
        return "redis".equalsIgnoreCase(senderType);
    }

    public String[] allowedOriginArray() {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return new String[]{"*"};
        }
        return allowedOrigins.split(",");
    }
}
