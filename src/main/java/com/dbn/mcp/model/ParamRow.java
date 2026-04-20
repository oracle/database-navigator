package com.dbn.mcp.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParamRow {
    private String name;
    private ParamType type;
    private String testValue;
    private String description;
    private boolean required;

    public ParamRow(String name) { this(name, ParamType.STRING, "", "", false); }
    public ParamRow(String name, ParamType type, String testValue) { this(name, type, testValue, "", false); }

    public ParamRow(String name, ParamType type, String testValue, String description, boolean required) {
        this.name = name;
        this.type = type != null ? type : ParamType.STRING;
        this.testValue = testValue != null ? testValue : "";
        this.description = description != null ? description : "";
        this.required = required;
    }
}
