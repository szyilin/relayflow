package com.relayflow.framework.mybatis.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Maps Java {@link String} JSON text to PostgreSQL {@code JSONB}.
 * <p>
 * Plain {@code varchar} bindings are rejected by PostgreSQL for {@code jsonb} columns;
 * this handler wraps values in {@link PGobject} with type {@code jsonb}.
 */
@MappedTypes(String.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class JsonbTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        PGobject json = new PGobject();
        json.setType("jsonb");
        json.setValue(parameter);
        ps.setObject(i, json);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return readJson(rs.getObject(columnName));
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return readJson(rs.getObject(columnIndex));
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return readJson(cs.getObject(columnIndex));
    }

    private static String readJson(Object value) throws SQLException {
        if (value == null) {
            return null;
        }
        if (value instanceof PGobject pgObject) {
            return pgObject.getValue();
        }
        return value.toString();
    }
}
