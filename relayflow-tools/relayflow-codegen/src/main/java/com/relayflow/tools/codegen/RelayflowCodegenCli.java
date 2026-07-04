package com.relayflow.tools.codegen;

import java.util.List;
import java.util.Map;

/**
 * CLI entry: generate DO/Mapper for explicitly listed tables into a temp output directory.
 */
public final class RelayflowCodegenCli {

    private RelayflowCodegenCli() {
    }

    public static void main(String[] args) throws Exception {
        CliOptions options = CliOptions.parse(args);
        CodegenConfig config = CodegenConfig.load(options.repoRoot(), options.configPath());
        CodegenConfig.ModuleConfig module = config.module(options.moduleKey());

        if (options.migrate()) {
            System.out.println("[relayflow-codegen] running Flyway migrate...");
            RelayflowCodegenEngine.migrate(config, options.repoRoot());
        }

        Map<Boolean, List<String>> partitioned = TenantTableDetector.partitionByTenantScope(
                config.jdbcUrl(), config.jdbcUsername(), config.jdbcPassword(), options.tables());

        List<String> baseTables = partitioned.get(false);
        List<String> tenantTables = partitioned.get(true);

        System.out.println("[relayflow-codegen] module=" + options.moduleKey()
                + " tables=" + options.tables()
                + " output=" + options.outputDir());

        RelayflowCodegenEngine.generate(config, module, baseTables, options.outputDir(), false);
        RelayflowCodegenEngine.generate(config, module, tenantTables, options.outputDir(), true);

        System.out.println("[relayflow-codegen] done. Review diff, then copy into module target/generated-sources/mybatis/ if OK.");
        System.out.println("[relayflow-codegen]   base tables:   " + baseTables);
        System.out.println("[relayflow-codegen]   tenant tables: " + tenantTables);
    }
}
