package com.relayflow.tools.codegen;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adds {@code JsonbTypeHandler} to generated DO fields backed by PostgreSQL {@code JSONB} columns.
 */
final class JsonbDoPostProcessor {

    private static final String JSONB_HANDLER = "com.relayflow.framework.mybatis.typehandler.JsonbTypeHandler";
    private static final String JSONB_HANDLER_IMPORT = "import " + JSONB_HANDLER + ";";
    private static final Pattern TABLE_FIELD_PATTERN = Pattern.compile(
            "@TableField\\(\"([a-z0-9_]+)\"\\)\\s*\\n(\\s*)private ([A-Za-z0-9_<>]+) ([a-zA-Z0-9_]+);");

    private JsonbDoPostProcessor() {
    }

    static void annotate(Path outputDir, CodegenConfig.ModuleConfig module, List<String> tables,
                         String jdbcUrl, String username, String password) throws Exception {
        Map<String, List<String>> jsonbColumnsByTable = loadJsonbColumns(tables, jdbcUrl, username, password);
        if (jsonbColumnsByTable.isEmpty()) {
            return;
        }

        String entityBase = module.packageParent().replace('.', '/') + "/"
                + module.entityPackage().replace('.', '/');
        Path entityDir = outputDir.resolve(entityBase);
        if (!Files.isDirectory(entityDir)) {
            return;
        }

        for (Map.Entry<String, List<String>> entry : jsonbColumnsByTable.entrySet()) {
            String tableName = entry.getKey();
            String className = toClassName(tableName) + "DO";
            Path doFile = entityDir.resolve(className + ".java");
            if (!Files.isRegularFile(doFile)) {
                continue;
            }
            patchDoFile(doFile, entry.getValue());
        }
    }

    private static Map<String, List<String>> loadJsonbColumns(List<String> tables, String jdbcUrl,
                                                              String username, String password) throws Exception {
        Map<String, List<String>> result = new LinkedHashMap<>();
        String sql = """
                SELECT table_name, column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND udt_name = 'jsonb'
                  AND table_name = ?
                ORDER BY ordinal_position
                """;

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String table : tables) {
                statement.setString(1, table.toLowerCase(Locale.ROOT));
                try (ResultSet rs = statement.executeQuery()) {
                    List<String> columns = new ArrayList<>();
                    while (rs.next()) {
                        columns.add(rs.getString("column_name"));
                    }
                    if (!columns.isEmpty()) {
                        result.put(table.toLowerCase(Locale.ROOT), columns);
                    }
                }
            }
        }
        return result;
    }

    private static void patchDoFile(Path doFile, List<String> jsonbColumns) throws Exception {
        String content = Files.readString(doFile);
        String updated = content;
        for (String column : jsonbColumns) {
            updated = patchColumn(updated, column);
        }
        if (!updated.equals(content)) {
            updated = ensureImport(updated);
            Files.writeString(doFile, updated);
        }
    }

    private static String patchColumn(String content, String columnName) {
        Matcher matcher = TABLE_FIELD_PATTERN.matcher(content);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            if (!columnName.equals(matcher.group(1))) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }
            String indent = matcher.group(2);
            String fieldName = matcher.group(4);
            String replacement = "@TableField(value = \"" + columnName + "\", typeHandler = JsonbTypeHandler.class)\n"
                    + indent + "private String " + fieldName + ";";
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String ensureImport(String content) {
        if (content.contains(JSONB_HANDLER_IMPORT)) {
            return content;
        }
        int tableFieldImport = content.indexOf("import com.baomidou.mybatisplus.annotation.TableField;");
        if (tableFieldImport >= 0) {
            int lineEnd = content.indexOf('\n', tableFieldImport);
            return content.substring(0, lineEnd + 1) + JSONB_HANDLER_IMPORT + "\n" + content.substring(lineEnd + 1);
        }
        int packageEnd = content.indexOf("import ");
        if (packageEnd >= 0) {
            return content.substring(0, packageEnd) + JSONB_HANDLER_IMPORT + "\n" + content.substring(packageEnd);
        }
        return content;
    }

    private static String toClassName(String tableName) {
        StringBuilder builder = new StringBuilder();
        for (String part : tableName.split("_")) {
            if (part.isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }
}
