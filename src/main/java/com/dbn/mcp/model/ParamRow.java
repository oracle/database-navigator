package com.dbn.mcp.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParamRow {
    private String name;
    private ParamType type;
    private String defaultValue;
    private String description;
    private boolean required;

    public ParamRow(String name) { this(name, ParamType.STRING, "", "", false); }
    public ParamRow(String name, ParamType type, String defaultValue) { this(name, type, defaultValue, "", false); }

    public ParamRow(String name, ParamType type, String defaultValue, String description, boolean required) {
        this.name = name;
        this.type = type != null ? type : ParamType.STRING;
        this.defaultValue = defaultValue != null ? defaultValue : "";
        this.description = description != null ? description : "";
        this.required = required;
    }
}
