package com.relayflow.tools.codegen;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Detects whether a table has a {@code tenant_id} column via JDBC metadata.
 */
public final class TenantTableDetector {

    private TenantTableDetector() {
    }

    public static Map<Boolean, List<String>> partitionByTenantScope(String jdbcUrl, String username,
                                                                    String password, List<String> tables) {
        Map<Boolean, List<String>> result = new LinkedHashMap<>();
        result.put(false, new ArrayList<>());
        result.put(true, new ArrayList<>());

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            for (String table : tables) {
                boolean tenantScoped = hasTenantIdColumn(connection, table);
                result.get(tenantScoped).add(table);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to inspect table metadata: " + ex.getMessage(), ex);
        }
        return result;
    }

    private static boolean hasTenantIdColumn(Connection connection, String table) throws SQLException {
        String sql = """
                SELECT 1
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                  AND column_name = 'tenant_id'
                LIMIT 1
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, table.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
