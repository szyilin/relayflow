package com.relayflow.tools.codegen;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Loads codegen config — module packages and enum column overrides only (no table lists).
 * Does not read Spring {@code application.yml} or Nacos.
 *
 * <p>Resolution order: {@code --config} path → {@code codegen.local.yml} (repo root) → classpath {@code codegen.yml}.
 */
public final class CodegenConfig {

    private final Map<String, Object> root;

    private CodegenConfig(Map<String, Object> root) {
        this.root = root != null ? root : Map.of();
    }

    public static CodegenConfig load(Path repoRoot, Path explicitConfigPath) throws IOException {
        if (explicitConfigPath != null && Files.isRegularFile(explicitConfigPath)) {
            return loadFromFile(explicitConfigPath);
        }
        Path local = repoRoot.resolve("codegen.local.yml").toAbsolutePath().normalize();
        if (Files.isRegularFile(local)) {
            return loadFromFile(local);
        }
        try (InputStream in = CodegenConfig.class.getResourceAsStream("/codegen.yml")) {
            if (in == null) {
                throw new IllegalStateException("classpath codegen.yml not found");
            }
            Yaml yaml = new Yaml();
            return new CodegenConfig(yaml.load(in));
        }
    }

    private static CodegenConfig loadFromFile(Path path) throws IOException {
        Yaml yaml = new Yaml();
        try (InputStream in = Files.newInputStream(path)) {
            return new CodegenConfig(yaml.load(in));
        }
    }

    public String jdbcUrl() {
        return envFirst(
                "RELAYFLOW_CODEGEN_JDBC_URL",
                "SPRING_DATASOURCE_URL",
                string(jdbcMap(), "url", "jdbc:postgresql://localhost:5432/relayflow"));
    }

    public String jdbcUsername() {
        return envFirst(
                "RELAYFLOW_CODEGEN_JDBC_USER",
                "POSTGRES_USER",
                string(jdbcMap(), "username", "relayflow"));
    }

    public String jdbcPassword() {
        return envFirst(
                "RELAYFLOW_CODEGEN_JDBC_PASSWORD",
                "POSTGRES_PASSWORD",
                string(jdbcMap(), "password", "relayflow"));
    }

    public Path flywayLocations(Path repoRoot) {
        String relative = string(nested(root, "flyway"), "locations",
                "relayflow-server/src/main/resources/db/migration");
        return repoRoot.resolve(relative).toAbsolutePath().normalize();
    }

    public ModuleConfig module(String moduleKey) {
        Map<String, Object> modules = map(root.get("modules"));
        Map<String, Object> module = map(modules.get(moduleKey));
        if (module.isEmpty()) {
            throw new IllegalArgumentException("Unknown module key in codegen.yml: " + moduleKey
                    + ". Available: " + modules.keySet());
        }
        return new ModuleConfig(
                string(module, "package-parent", ""),
                string(module, "entity-package", "dal.dataobject"),
                string(module, "mapper-package", "dal.mysql"),
                string(module, "enum-package", "")
        );
    }

    public Map<String, String> enumColumns() {
        Map<String, Object> raw = map(root.get("enum-columns"));
        if (raw.isEmpty()) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        raw.forEach((key, value) -> result.put(key.toLowerCase(), Objects.toString(value, "")));
        return Collections.unmodifiableMap(result);
    }

    private Map<String, Object> jdbcMap() {
        return map(root.get("jdbc"));
    }

    private static String envFirst(String... keysAndFallback) {
        for (int i = 0; i < keysAndFallback.length - 1; i++) {
            String value = System.getenv(keysAndFallback[i]);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return keysAndFallback[keysAndFallback.length - 1];
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        if (value instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return Map.of();
    }

    private static Map<String, Object> nested(Map<String, Object> parent, String key) {
        return map(parent.get(key));
    }

    private static String string(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? Objects.toString(value) : defaultValue;
    }

    public record ModuleConfig(
            String packageParent,
            String entityPackage,
            String mapperPackage,
            String enumPackage
    ) {
    }
}
