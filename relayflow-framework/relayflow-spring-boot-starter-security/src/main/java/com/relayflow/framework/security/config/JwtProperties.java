package com.relayflow.framework.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "relayflow.security.jwt")
public class JwtProperties {

    private String secret = "dev-only-change-me-in-production";

    private long accessTokenExpireSeconds = 7200L;
}
