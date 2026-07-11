package com.relayflow.framework.security.config;

import com.relayflow.common.encrypt.AesGcmEncryptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import java.util.Base64;

@AutoConfiguration
@EnableConfigurationProperties(EncryptProperties.class)
public class EncryptAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "relayflow.encrypt", name = "aes-key")
    public AesGcmEncryptor aesGcmEncryptor(EncryptProperties properties) {
        if (!StringUtils.hasText(properties.getAesKey())) {
            throw new IllegalStateException("relayflow.encrypt.aes-key must not be blank");
        }
        byte[] keyBytes = Base64.getDecoder().decode(properties.getAesKey().trim());
        return new AesGcmEncryptor(keyBytes);
    }
}
