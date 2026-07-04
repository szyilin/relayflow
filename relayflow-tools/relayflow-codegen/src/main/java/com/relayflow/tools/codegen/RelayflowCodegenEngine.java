package com.relayflow.tools.codegen;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.TemplateType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import org.flywaydb.core.Flyway;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Runs MyBatis-Plus generator for a user-supplied table list.
 */
public final class RelayflowCodegenEngine {

    private static final String AUTHOR = "relayflow-codegen";

    private static final List<String> BASE_SUPER_COLUMNS = List.of(
            "id", "creator", "create_time", "updater", "update_time", "deleted"
    );

    private static final List<String> TENANT_SUPER_COLUMNS = List.of(
            "id", "tenant_id", "creator", "create_time", "updater", "update_time", "deleted"
    );

    private RelayflowCodegenEngine() {
    }

    public static void migrate(CodegenConfig config, Path repoRoot) {
        Path flywayLocations = config.flywayLocations(repoRoot);
        Flyway.configure()
                .dataSource(config.jdbcUrl(), config.jdbcUsername(), config.jdbcPassword())
                .locations("filesystem:" + flywayLocations)
                .load()
                .migrate();
    }

    public static void generate(CodegenConfig config, CodegenConfig.ModuleConfig module,
                                List<String> tables, Path outputDir, boolean tenantScoped) throws Exception {
        if (tables.isEmpty()) {
            return;
        }

        Files.createDirectories(outputDir);

        String superClass = tenantScoped
                ? "com.relayflow.common.dal.TenantBaseDO"
                : "com.relayflow.common.dal.BaseDO";
        List<String> superColumns = tenantScoped ? TENANT_SUPER_COLUMNS : BASE_SUPER_COLUMNS;

        RelayflowColumnTypeConvertHandler typeHandler =
                new RelayflowColumnTypeConvertHandler(config.enumColumns(), module.enumPackage());

        FastAutoGenerator.create(config.jdbcUrl(), config.jdbcUsername(), config.jdbcPassword())
                .globalConfig(builder -> builder
                        .author(AUTHOR)
                        .disableOpenDir()
                        .outputDir(outputDir.toString()))
                .dataSourceConfig(builder -> builder.typeConvertHandler(typeHandler))
                .packageConfig(builder -> builder
                        .parent(module.packageParent())
                        .entity(module.entityPackage())
                        .mapper(module.mapperPackage()))
                .strategyConfig(builder -> builder
                        .addInclude(tables)
                        .entityBuilder()
                        .enableLombok()
                        .enableTableFieldAnnotation()
                        .enableFileOverride()
                        .formatFileName("%sDO")
                        .superClass(superClass)
                        .addSuperEntityColumns(superColumns)
                        .mapperBuilder()
                        .enableMapperAnnotation()
                        .enableFileOverride()
                        .formatMapperFileName("%sMapper")
                        .disableMapperXml())
                .templateConfig(builder -> builder
                        .disable(TemplateType.SERVICE)
                        .disable(TemplateType.SERVICE_IMPL)
                        .disable(TemplateType.CONTROLLER)
                        .disable(TemplateType.XML))
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();

        pruneNonDalArtifacts(outputDir, module);
    }

    /**
     * MP 3.5 may still emit controller/service stubs even when templates are disabled; remove them.
     */
    private static void pruneNonDalArtifacts(Path outputDir, CodegenConfig.ModuleConfig module) throws Exception {
        String base = module.packageParent().replace('.', '/');
        deleteIfExists(outputDir.resolve(base + "/controller"));
        deleteIfExists(outputDir.resolve(base + "/service"));
        deleteIfExists(outputDir.resolve(base + "/mapper"));
    }

    private static void deleteIfExists(Path dir) throws Exception {
        if (Files.isDirectory(dir)) {
            try (var walk = Files.walk(dir)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }
}
