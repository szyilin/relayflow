package com.relayflow.framework.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "relayflow.encrypt")
public class EncryptProperties {

    /**
     * Base64-encoded AES-256 key (32 raw bytes).
     */
    private String aesKey;
}
