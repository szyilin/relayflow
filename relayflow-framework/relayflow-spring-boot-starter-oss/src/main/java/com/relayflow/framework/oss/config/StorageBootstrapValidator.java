package com.relayflow.framework.oss.config;

import com.relayflow.framework.oss.core.ObjectStorageClientFactory;
import com.relayflow.framework.oss.core.ObjectStorageProviderType;
import com.relayflow.framework.oss.core.model.StorageProviderConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@RequiredArgsConstructor
public class StorageBootstrapValidator implements ApplicationRunner {

    private final StorageProperties properties;
    private final ObjectStorageClientFactory clientFactory;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        ObjectStorageProviderType defaultProvider = properties.getDefaultProvider();
        if (defaultProvider == null) {
            throw new IllegalStateException("relayflow.storage.default-provider is required");
        }

        if (defaultProvider == ObjectStorageProviderType.LOCAL) {
            if (!properties.isAllowLocal() || !isDevProfile()) {
                throw new IllegalStateException(
                        "relayflow.storage.default-provider=local is only allowed in dev with allow-local=true");
            }
            return;
        }

        properties.validateBootstrapBlocks();
        StorageProviderConfig bootstrapConfig = properties.toBootstrapConfig();

        if (properties.isValidateOnStartup()) {
            clientFactory.getClient(defaultProvider).checkConnectivity(bootstrapConfig);
        }
    }

    private boolean isDevProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "dev".equalsIgnoreCase(profile) || "local".equalsIgnoreCase(profile));
    }
}
