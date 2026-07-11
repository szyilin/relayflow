package com.relayflow.framework.oss.config;

import com.relayflow.framework.oss.core.ObjectStorageClientFactory;
import com.relayflow.framework.oss.core.model.StorageProviderConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@AutoConfiguration
@EnableConfigurationProperties(StorageProperties.class)
public class OssAutoConfiguration {

    @Bean
    public ObjectStorageClientFactory objectStorageClientFactory() {
        return new ObjectStorageClientFactory();
    }

    @Bean
    @ConditionalOnProperty(prefix = "relayflow.storage", name = "default-provider", havingValue = "minio")
    public StorageProviderConfig storageBootstrapConfig(StorageProperties properties) {
        properties.validateBootstrapBlocks();
        return properties.toBootstrapConfig();
    }

    @Bean
    public StorageBootstrapValidator storageBootstrapValidator(StorageProperties properties,
                                                               ObjectStorageClientFactory clientFactory,
                                                               Environment environment) {
        return new StorageBootstrapValidator(properties, clientFactory, environment);
    }
}
