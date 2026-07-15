package com.relayflow.tools.codegen;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * CLI options for selective table codegen.
 */
public final class CliOptions {

    private static final DateTimeFormatter OUT_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final String moduleKey;
    private final List<String> tables;
    private final Path outputDir;
    private final Path configPath;
    private final boolean migrate;
    private final Path repoRoot;

    private CliOptions(String moduleKey, List<String> tables, Path outputDir, Path configPath,
                       boolean migrate, Path repoRoot) {
        this.moduleKey = moduleKey;
        this.tables = List.copyOf(tables);
        this.outputDir = outputDir;
        this.configPath = configPath;
        this.migrate = migrate;
        this.repoRoot = repoRoot;
    }

    public static CliOptions parse(String[] args) {
        String moduleKey = null;
        List<String> tables = new ArrayList<>();
        Path outputDir = null;
        Path configPath = null;
        boolean migrate = false;
        Path repoRoot = Path.of(System.getProperty("relayflow.repo.root", ".")).toAbsolutePath().normalize();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--help", "-h" -> {
                    printHelp();
                    System.exit(0);
                }
                case "--module", "-m" -> moduleKey = requireValue(args, ++i, arg);
                case "--tables", "-t" -> tables.addAll(splitTables(requireValue(args, ++i, arg)));
                case "--output", "-o" -> outputDir = Path.of(requireValue(args, ++i, arg));
                case "--config" -> configPath = Path.of(requireValue(args, ++i, arg));
                case "--migrate" -> migrate = true;
                case "--repo-root" -> repoRoot = Path.of(requireValue(args, ++i, arg)).toAbsolutePath().normalize();
                default -> throw new IllegalArgumentException("Unknown argument: " + arg);
            }
        }

        if (moduleKey == null || moduleKey.isBlank()) {
            throw new IllegalArgumentException("--module is required");
        }
        if (tables.isEmpty()) {
            throw new IllegalArgumentException("--tables is required (comma-separated table names)");
        }
        if (outputDir == null) {
            outputDir = repoRoot.resolve(".relayflow/codegen-out/" + OUT_TS.format(LocalDateTime.now()));
        } else {
            outputDir = outputDir.toAbsolutePath().normalize();
        }

        return new CliOptions(moduleKey, tables, outputDir, configPath, migrate, repoRoot);
    }

    private static void printHelp() {
        System.out.println("""
                RelayFlow MyBatis-Plus codegen — selective tables -> temp DIFF REFERENCE

                Emits untrimmed *DO.java, *Mapper.java, *Mapper.xml for comparison against
                managed files under *-biz/src/ (see docs/dev/codegen.md). Custom SQL stays in
                *ExtMapper* / *PublicMapper* (never merge those from this output).

                Usage:
                  relayflow-codegen --module <key> --tables <t1,t2,...> [options]

                Options:
                  -m, --module <key>     Module key from codegen.yml (system, infra, im, task)
                  -t, --tables <list>    Comma-separated PostgreSQL table names (required)
                  -o, --output <dir>     Output directory (default: .relayflow/codegen-out/<timestamp>/)
                  --config <file>        Custom YAML (else codegen.local.yml, else classpath codegen.yml)
                  --migrate              Run Flyway before generating
                  --repo-root <dir>      Repository root (default: . or relayflow.repo.root property)
                  -h, --help             Show help

                JDBC env (first match wins): RELAYFLOW_CODEGEN_JDBC_* or POSTGRES_* / SPRING_DATASOURCE_URL

                Examples:
                  ./scripts/codegen.sh -m system -t sys_user,sys_dept
                  ./scripts/codegen.sh -m system -t sys_role --migrate -o /tmp/rf-out
                """);
    }

    private static String requireValue(String[] args, int index, String flag) {
        if (index >= args.length) {
            throw new IllegalArgumentException("Missing value for " + flag);
        }
        return args[index];
    }

    private static List<String> splitTables(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    public String moduleKey() {
        return moduleKey;
    }

    public List<String> tables() {
        return tables;
    }

    public Path outputDir() {
        return outputDir;
    }

    public Path configPath() {
        return configPath;
    }

    public boolean migrate() {
        return migrate;
    }

    public Path repoRoot() {
        return repoRoot;
    }
}
