package com.dbn.mcp.model;

public enum ParamType {
    STRING("string"),
    INTEGER("integer"),
    NUMBER("number"),
    BOOLEAN("boolean");

    private final String yamlType;

    ParamType(String yamlType) {
        this.yamlType = yamlType;
    }

    public String getYamlType() {
        return yamlType;
    }

    public static ParamType fromYamlType(String yamlType) {
        for (ParamType pt : values()) {
            if (pt.yamlType.equals(yamlType)) return pt;
        }
        return STRING;
    }
}
