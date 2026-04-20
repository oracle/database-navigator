package com.dbn.mcp.model;

public enum ParamType {
    STRING("string", null),
    INTEGER("integer", null),
    NUMBER("number", null),
    BOOLEAN("boolean", null),
    DATE("string", "date");

    private final String schemaType;
    private final String schemaFormat;

    ParamType(String schemaType, String schemaFormat) {
        this.schemaType = schemaType;
        this.schemaFormat = schemaFormat;
    }

    public String getSchemaType() {
        return schemaType;
    }

    public String getSchemaFormat() {
        return schemaFormat;
    }

    public static ParamType fromYamlType(String yamlType) {
        return fromYamlType(yamlType, null);
    }

    public static ParamType fromYamlType(String yamlType, String yamlFormat) {
        if ("string".equalsIgnoreCase(yamlType) && "date".equalsIgnoreCase(yamlFormat)) {
            return DATE;
        }

        for (ParamType pt : values()) {
            if (pt.schemaType.equalsIgnoreCase(yamlType) && pt.schemaFormat == null) return pt;
        }
        return STRING;
    }
}
