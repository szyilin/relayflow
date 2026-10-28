package com.relayflow.module.infra.service.storage;

import com.relayflow.framework.job.config.JobProperties;
import com.relayflow.framework.oss.config.StorageProperties;
import com.relayflow.framework.security.config.EncryptProperties;
import com.relayflow.framework.security.config.JwtProperties;
import com.relayflow.framework.tenant.config.TenantProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Load the server's actual config data without starting databases or business beans. */
class LocalDockerProfileTest {

    @Test
    void localProfileUsesContainerAddressesAndPersistedSecretsWithoutDevDefaults() {
        Map<String, Object> values = localEnvironment();
        values.put("REDIS_PORT", "16379"); // Published host port must not change the internal port.
        try (var context = load(values)) {
            var env = context.getEnvironment();
            assertEquals(List.of("local-docker"), List.of(env.getActiveProfiles()));
            assertEquals("jdbc:postgresql://postgres:5432/relayflow", env.getProperty("spring.datasource.url"));
            assertEquals("persisted-db-password", env.getProperty("spring.datasource.password"));
            assertEquals("redis", env.getProperty("spring.data.redis.host"));
            assertEquals("6379", env.getProperty("spring.data.redis.port"));
            var tenant = bind(context, "relayflow.tenant", TenantProperties.class);
            assertFalse(tenant.isEnabled());
            assertFalse(tenant.isAllowOpenRegister());
            var storage = bind(context, "relayflow.storage", StorageProperties.class);
            storage.validateBootstrapBlocks();
            assertEquals("http://minio:9000", storage.getMinio().getEndpoint());
            assertEquals("http://localhost:19000", storage.getMinio().getPublicEndpoint());
            assertEquals("persisted-minio-password", storage.getMinio().getSecretKey());
            assertEquals(values.get("RELAYFLOW_JWT_SECRET"), bind(context, "relayflow.security.jwt", JwtProperties.class).getSecret());
            assertEquals(values.get("RELAYFLOW_ENCRYPT_AES_KEY"), bind(context, "relayflow.encrypt", EncryptProperties.class).getAesKey());
            JobProperties job = bind(context, "relayflow.job", JobProperties.class);
            assertTrue(job.isEnabled());
            assertEquals("http://xxl-job-admin:8080/xxl-job-admin", job.getAdminAddresses());
            assertEquals("http://server:9999/", job.getExecutor().getAddress());
            assertEquals("persisted-xxl-token", job.getAccessToken());
            assertEquals("/app/logs/xxl-job/jobhandler", job.getExecutor().getLogPath());
            assertEquals("ws://localhost:7880", env.getProperty("relayflow.rtc.livekit.url"));
            assertEquals("http://livekit:7880", env.getProperty("relayflow.rtc.livekit.api-url"));
            assertEquals("persisted-livekit-secret", env.getProperty("relayflow.rtc.livekit.api-secret"));
        }
    }

    @Test
    void publishedPortsOnlyAffectBrowserUrlsAndExecutorPortUsesItsContainerValue() {
        Map<String, Object> values = localEnvironment();
        values.putAll(Map.of("POSTGRES_DB", "custom_db", "MINIO_API_PORT", "29000", "LIVEKIT_PORT", "17880",
                "RELAYFLOW_JOB_EXECUTOR_PORT", "19999"));
        try (var context = load(values)) {
            var env = context.getEnvironment();
            assertEquals("jdbc:postgresql://postgres:5432/custom_db", env.getProperty("spring.datasource.url"));
            assertEquals("http://localhost:29000", env.getProperty("relayflow.storage.minio.public-endpoint"));
            assertEquals("http://minio:9000", env.getProperty("relayflow.storage.minio.endpoint"));
            assertEquals("ws://localhost:17880", env.getProperty("relayflow.rtc.livekit.url"));
            assertEquals("http://livekit:7880", env.getProperty("relayflow.rtc.livekit.api-url"));
            assertEquals("http://server:19999/", bind(context, "relayflow.job", JobProperties.class).getExecutor().getAddress());
        }
    }

    @Test
    void explicitEnvironmentOverridesReachAllContainerConnections() {
        Map<String, Object> values = localEnvironment();
        values.putAll(Map.of("SPRING_DATASOURCE_URL", "jdbc:postgresql://custom-pg:5433/db",
                "SPRING_REDIS_HOST", "custom-redis", "SPRING_REDIS_PORT", "6380",
                "RELAYFLOW_MINIO_ENDPOINT", "http://custom-minio:9000", "RELAYFLOW_MINIO_PUBLIC_ENDPOINT", "https://files.example",
                "RELAYFLOW_MINIO_REGION", "us-west-2", "RELAYFLOW_LIVEKIT_URL", "wss://rtc.example",
                "RELAYFLOW_LIVEKIT_API_URL", "http://custom-livekit:7880",
                "RELAYFLOW_JOB_ADMIN_ADDRESSES", "http://custom-admin:8080/xxl-job-admin",
                "RELAYFLOW_JOB_EXECUTOR_ADDRESS", "http://custom-server:9999/"));
        try (var context = load(values)) {
            var env = context.getEnvironment();
            assertEquals("jdbc:postgresql://custom-pg:5433/db", env.getProperty("spring.datasource.url"));
            assertEquals("custom-redis", env.getProperty("spring.data.redis.host"));
            assertEquals("6380", env.getProperty("spring.data.redis.port"));
            StorageProperties storage = bind(context, "relayflow.storage", StorageProperties.class);
            assertEquals("http://custom-minio:9000", storage.getMinio().getEndpoint());
            assertEquals("https://files.example", storage.getMinio().getPublicEndpoint());
            assertEquals("us-west-2", storage.getMinio().getRegion());
            assertEquals("wss://rtc.example", env.getProperty("relayflow.rtc.livekit.url"));
            assertEquals("http://custom-livekit:7880", env.getProperty("relayflow.rtc.livekit.api-url"));
            JobProperties job = bind(context, "relayflow.job", JobProperties.class);
            assertEquals("http://custom-admin:8080/xxl-job-admin", job.getAdminAddresses());
            assertEquals("http://custom-server:9999/", job.getExecutor().getAddress());
        }
    }

    @Test
    void tenantOverridesRemainAvailableWithoutActivatingDev() {
        Map<String, Object> values = localEnvironment();
        values.putAll(Map.of("RELAYFLOW_TENANT_ENABLED", "true", "RELAYFLOW_TENANT_ALLOW_OPEN_REGISTER", "true",
                "RELAYFLOW_TENANT_DEFAULT_ID", "42"));
        try (var context = load(values)) {
            TenantProperties tenant = bind(context, "relayflow.tenant", TenantProperties.class);
            assertTrue(tenant.isEnabled());
            assertTrue(tenant.isAllowOpenRegister());
            assertEquals(42L, tenant.getDefaultId());
            assertFalse(List.of(context.getEnvironment().getActiveProfiles()).contains("dev"));
        }
    }

    @Test
    void missingPersistedCredentialsNeverFallBackToDevelopmentValues() {
        Map<String, String> required = Map.of("POSTGRES_PASSWORD", "spring.datasource.password",
                "MINIO_ROOT_PASSWORD", "relayflow.storage.minio.secret-key",
                "RELAYFLOW_JWT_SECRET", "relayflow.security.jwt.secret",
                "RELAYFLOW_ENCRYPT_AES_KEY", "relayflow.encrypt.aes-key",
                "RELAYFLOW_LIVEKIT_API_SECRET", "relayflow.rtc.livekit.api-secret",
                "XXL_JOB_ACCESS_TOKEN", "relayflow.job.access-token");
        required.forEach((variable, property) -> {
            Map<String, Object> values = localEnvironment();
            values.remove(variable);
            try (var context = load(values)) {
                var error = assertThrows(IllegalArgumentException.class, () -> context.getEnvironment().getRequiredProperty(property));
                assertTrue(error.getMessage().contains(variable));
            }
        });
    }

    @Test
    void jobTokenCanBeExplicitlyInjectedUsingTheApplicationVariable() {
        Map<String, Object> values = localEnvironment();
        values.remove("XXL_JOB_ACCESS_TOKEN");
        values.put("RELAYFLOW_JOB_ACCESS_TOKEN", "explicit-app-token");
        try (var context = load(values)) {
            assertEquals("explicit-app-token", bind(context, "relayflow.job", JobProperties.class).getAccessToken());
        }
    }

    @Test
    void originalDevProfileRetainsItsHostDevelopmentBehavior() {
        Map<String, Object> values = localEnvironment();
        values.put("SPRING_PROFILES_ACTIVE", "dev");
        try (var context = load(values)) {
            assertTrue(bind(context, "relayflow.tenant", TenantProperties.class).isEnabled());
            assertEquals("http://host.docker.internal:9999/", bind(context, "relayflow.job", JobProperties.class).getExecutor().getAddress());
        }
    }

    private Map<String, Object> localEnvironment() {
        return new HashMap<>(Map.of("SPRING_PROFILES_ACTIVE", "local-docker", "POSTGRES_USER", "relayflow",
                "POSTGRES_PASSWORD", "persisted-db-password", "MINIO_ROOT_USER", "persisted-minio-user",
                "MINIO_ROOT_PASSWORD", "persisted-minio-password", "RELAYFLOW_JWT_SECRET", "a".repeat(64),
                "RELAYFLOW_ENCRYPT_AES_KEY", "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                "RELAYFLOW_LIVEKIT_API_KEY", "persisted-livekit-key", "RELAYFLOW_LIVEKIT_API_SECRET", "persisted-livekit-secret",
                "XXL_JOB_ACCESS_TOKEN", "persisted-xxl-token"));
    }

    private ConfigurableApplicationContext load(Map<String, Object> values) {
        Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.isDirectory(root.resolve("relayflow-server/src/main/resources"))) {
            root = root.getParent();
        }
        assertNotNull(root, "Locate actual server configuration from the checkout");
        var environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().addFirst(new SystemEnvironmentPropertySource("test-env", values));
        var app = new SpringApplication(ConfigOnly.class);
        app.setEnvironment(environment);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.setBannerMode(Banner.Mode.OFF);
        app.setLogStartupInfo(false);
        app.setRegisterShutdownHook(false);
        return app.run("--spring.config.location=" + root.resolve("relayflow-server/src/main/resources/").toUri(),
                "--logging.level.root=OFF");
    }

    private <T> T bind(ConfigurableApplicationContext context, String prefix, Class<T> type) {
        return Binder.get(context.getEnvironment()).bind(prefix, Bindable.of(type)).get();
    }

    @Configuration(proxyBeanMethods = false)
    static class ConfigOnly {}
}
