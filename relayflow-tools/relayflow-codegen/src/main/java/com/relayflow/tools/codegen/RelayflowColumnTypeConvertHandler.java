package com.relayflow.tools.codegen;

import com.baomidou.mybatisplus.generator.config.GlobalConfig;
import com.baomidou.mybatisplus.generator.config.po.TableField;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.baomidou.mybatisplus.generator.config.rules.IColumnType;
import com.baomidou.mybatisplus.generator.type.TypeRegistry;

import java.util.Locale;
import java.util.Map;

/**
 * PostgreSQL column type mapping; enum overrides loaded from {@code codegen.yml}.
 */
public class RelayflowColumnTypeConvertHandler implements com.baomidou.mybatisplus.generator.type.ITypeConvertHandler {

    private final Map<String, String> enumColumns;
    private final String enumPackage;

    public RelayflowColumnTypeConvertHandler(Map<String, String> enumColumns, String enumPackage) {
        this.enumColumns = enumColumns != null ? enumColumns : Map.of();
        this.enumPackage = enumPackage != null ? enumPackage : "";
    }

    @Override
    public IColumnType convert(GlobalConfig globalConfig, TypeRegistry typeRegistry, TableField.MetaInfo metaInfo) {
        String key = metaInfo.getTableName().toLowerCase(Locale.ROOT) + "."
                + metaInfo.getColumnName().toLowerCase(Locale.ROOT);
        String enumName = enumColumns.get(key);
        if (enumName != null && !enumName.isBlank()) {
            return enumType(enumPackage, enumName);
        }

        if ("timestamptz".equalsIgnoreCase(metaInfo.getTypeName())) {
            return simpleType("OffsetDateTime", "java.time.OffsetDateTime");
        }

        if ("int2".equalsIgnoreCase(metaInfo.getTypeName()) || "smallint".equalsIgnoreCase(metaInfo.getTypeName())) {
            return DbColumnType.INTEGER;
        }

        return typeRegistry.getColumnType(metaInfo);
    }

    private static IColumnType enumType(String enumPackage, String enumName) {
        String fqcn = enumPackage + "." + enumName;
        return new IColumnType() {
            @Override
            public String getType() {
                return enumName;
            }

            @Override
            public String getPkg() {
                return fqcn;
            }
        };
    }

    private static IColumnType simpleType(String simpleName, String pkg) {
        return new IColumnType() {
            @Override
            public String getType() {
                return simpleName;
            }

            @Override
            public String getPkg() {
                return pkg;
            }
        };
    }
}
