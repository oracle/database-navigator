package com.dbn.mcp.model;

import lombok.Getter;
import org.jetbrains.annotations.NonNls;

@Getter
public enum McpToolParamType {
    STRING("string", null),
    INTEGER("integer", null),
    NUMBER("number", null),
    BOOLEAN("boolean", null),
    DATE("string", "date");

    private final String schemaType;
    private final String schemaFormat;

    McpToolParamType(@NonNls String schemaType, String schemaFormat) {
        this.schemaType = schemaType;
        this.schemaFormat = schemaFormat;
    }

    public static McpToolParamType fromYamlType(String yamlType) {
        return fromYamlType(yamlType, null);
    }

    public static McpToolParamType fromYamlType(String yamlType, String yamlFormat) {
        if ("string".equalsIgnoreCase(yamlType) && "date".equalsIgnoreCase(yamlFormat)) {
            return DATE;
        }

        for (McpToolParamType pt : values()) {
            if (pt.schemaType.equalsIgnoreCase(yamlType) && pt.schemaFormat == null) return pt;
        }
        return STRING;
    }
}
