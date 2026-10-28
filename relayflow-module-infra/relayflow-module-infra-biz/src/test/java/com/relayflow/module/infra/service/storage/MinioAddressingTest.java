package com.relayflow.module.infra.service.storage;

import com.relayflow.framework.oss.config.StorageProperties;
import com.relayflow.framework.oss.core.ObjectStorageProviderType;
import com.relayflow.framework.oss.core.model.StorageProviderConfig;
import com.relayflow.framework.oss.minio.MinioObjectStorageClient;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** SDK tests live in infra-biz, reusing its existing test dependencies. */
class MinioAddressingTest {

    private final MinioObjectStorageClient client = new MinioObjectStorageClient();

    @Test
    void bindsOptionalDeploymentPropertiesAndPreservesTenantCredentials() {
        var source = new MapConfigurationPropertySource(Map.of(
                "relayflow.storage.default-provider", "minio",
                "relayflow.storage.minio.endpoint", "http://MINIO:9000/",
                "relayflow.storage.minio.public-endpoint", "http://localhost:19000",
                "relayflow.storage.minio.region", "us-east-1"));
        StorageProperties properties = new Binder(source)
                .bind("relayflow.storage", Bindable.of(StorageProperties.class)).get();
        StorageProviderConfig tenant = config("http://minio:9000");
        StorageProviderConfig mapped = properties.resolveRuntimeConfig(tenant);
        assertEquals("http://MINIO:9000/", mapped.getEndpoint());
        assertEquals("http://localhost:19000", mapped.getPublicEndpoint());
        assertEquals("us-east-1", mapped.getRegion());
        assertEquals(tenant.getAccessKey(), mapped.getAccessKey());
        assertEquals(tenant.getSecretKey(), mapped.getSecretKey());
        assertEquals(tenant.getBucket(), mapped.getBucket());
        assertEquals(tenant.getPathPrefix(), mapped.getPathPrefix());
        assertNull(tenant.getPublicEndpoint(), "Resolution must not mutate persisted configuration");
        assertEquals("http://localhost:19000", properties.toBootstrapConfig().getPublicEndpoint());
    }

    @Test
    void mapsPublicOriginAndNormalizesDefaultPortsButLeavesIndependentOriginsAlone() {
        StorageProperties properties = managedProperties("http://minio:80/", "https://files.example:443/");
        assertEquals("http://minio:80/", properties.resolveRuntimeConfig(config("https://FILES.example")).getEndpoint());
        assertNotNull(properties.resolveRuntimeConfig(config("http://MINIO")).getPublicEndpoint());
        for (String endpoint : List.of("http://minio:9000", "https://minio", "http://minio.evil",
                "https://files.example/path", "https://files.example?other=1", "http://user@minio")) {
            StorageProviderConfig independent = config(endpoint);
            assertSame(independent, properties.resolveRuntimeConfig(independent));
        }
    }

    @Test
    void noPublicOriginPreservesLegacyConfigAndDualAddressRequiresRegion() {
        StorageProperties properties = new StorageProperties();
        StorageProviderConfig legacy = config("http://minio:9000");
        assertSame(legacy, properties.resolveRuntimeConfig(legacy));
        properties.getMinio().setEndpoint("http://minio:9000");
        properties.getMinio().setPublicEndpoint("http://localhost:19000");
        assertThrows(IllegalStateException.class, () -> properties.resolveRuntimeConfig(legacy));
        assertThrows(IllegalStateException.class, () -> client.createPresignedGet(
                legacy.toBuilder().publicEndpoint("http://browser.invalid:19000").region(null).build(),
                "file.txt", Duration.ofMinutes(5)));
    }

    @Test
    void putAndGetSignThePublicHostWithoutAnyNetworkDiscovery() throws Exception {
        StorageProviderConfig config = config("http://internal.invalid:9000").toBuilder()
                .publicEndpoint("http://browser.invalid:19000").build();
        String put = client.createPresignedPut(config, "space file.txt", "text/plain", Duration.ofMinutes(5)).getUploadUrl();
        String get = client.createPresignedGet(config, "space file.txt", Duration.ofMinutes(5));
        for (String method : List.of("PUT", "GET")) {
            URI uri = URI.create(method.equals("PUT") ? put : get);
            assertEquals("browser.invalid:19000", uri.getRawAuthority());
            assertEquals("/tenant-bucket/prefix/space%20file.txt", uri.getRawPath());
            assertSignature(method, uri, config.getSecretKey());
        }
    }

    @Test
    void connectivityHeadAndDeleteUseInternalAddress() throws Exception {
        List<String> requests = new CopyOnWriteArrayList<>();
        HttpServer server = fixture(requests);
        try {
            StorageProviderConfig config = config("http://127.0.0.1:" + server.getAddress().getPort()).toBuilder()
                    .publicEndpoint("http://unreachable.invalid:19000").build();
            client.checkConnectivity(config);
            assertEquals(3, client.headObject(config, "file.txt").getSize());
            client.deleteObject(config, "file.txt");
            assertEquals(List.of("HEAD /tenant-bucket", "HEAD /tenant-bucket/prefix/file.txt",
                    "DELETE /tenant-bucket/prefix/file.txt"), requests);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void legacySingleAddressStillDiscoversRegionThroughTheOriginalEndpoint() throws Exception {
        List<String> requests = new CopyOnWriteArrayList<>();
        HttpServer server = fixture(requests);
        try {
            StorageProviderConfig legacy = config("http://127.0.0.1:" + server.getAddress().getPort())
                    .toBuilder().region(null).build();
            URI url = URI.create(client.createPresignedGet(legacy, "file.txt", Duration.ofMinutes(5)));
            assertEquals(server.getAddress().getPort(), url.getPort());
            assertTrue(requests.stream().anyMatch(request -> request.startsWith("GET /tenant-bucket")));
            assertSignature("GET", url, legacy.getSecretKey());
        } finally {
            server.stop(0);
        }
    }

    private HttpServer fixture(List<String> requests) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String method = exchange.getRequestMethod();
            requests.add(method + " " + exchange.getRequestURI().getRawPath());
            exchange.getResponseHeaders().set("x-amz-bucket-region", "us-east-1");
            if (method.equals("GET")) {
                byte[] body = ("<LocationConstraint xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">"
                        + "us-east-1</LocationConstraint>").getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } else {
                exchange.getResponseHeaders().set("Content-Length", method.equals("DELETE") ? "0" : "3");
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.getResponseHeaders().set("Last-Modified", "Wed, 28 Oct 2026 00:00:00 GMT");
                exchange.getResponseHeaders().set("ETag", "\"test-etag\"");
                exchange.sendResponseHeaders(method.equals("DELETE") ? 204 : 200, -1);
            }
            exchange.close();
        });
        server.start();
        return server;
    }

    static StorageProperties managedProperties(String internal, String external) {
        StorageProperties properties = new StorageProperties();
        properties.setDefaultProvider(ObjectStorageProviderType.MINIO);
        properties.getMinio().setEndpoint(internal);
        properties.getMinio().setPublicEndpoint(external);
        properties.getMinio().setRegion("us-east-1");
        return properties;
    }

    static StorageProviderConfig config(String endpoint) {
        return StorageProviderConfig.builder().providerType(ObjectStorageProviderType.MINIO)
                .endpoint(endpoint).region("us-east-1").accessKey("test-access")
                .secretKey("test-secret-for-signature-verification").bucket("tenant-bucket")
                .pathPrefix("prefix/").build();
    }

    /** Independently verify SigV4; changing the host after signing must not pass. */
    private void assertSignature(String method, URI uri, String secret) throws Exception {
        List<String> pairs = new ArrayList<>(Arrays.asList(uri.getRawQuery().split("&")));
        Map<String, String> query = pairs.stream().map(pair -> pair.split("=", 2))
                .collect(Collectors.toMap(pair -> pair[0], pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));
        pairs.removeIf(pair -> pair.startsWith("X-Amz-Signature="));
        pairs.sort(String::compareTo);
        String scope = query.get("X-Amz-Credential").split("/", 2)[1];
        String[] parts = scope.split("/");
        String canonical = method + "\n" + uri.getRawPath() + "\n" + String.join("&", pairs)
                + "\nhost:" + uri.getRawAuthority() + "\n\nhost\nUNSIGNED-PAYLOAD";
        String toSign = "AWS4-HMAC-SHA256\n" + query.get("X-Amz-Date") + "\n" + scope + "\n"
                + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        byte[] key = ("AWS4" + secret).getBytes(StandardCharsets.UTF_8);
        for (String part : parts) {
            key = hmac(key, part);
        }
        assertEquals(query.get("X-Amz-Signature"), HexFormat.of().formatHex(hmac(key, toSign)));
    }

    private byte[] hmac(byte[] key, String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }
}
