package com.dbn.mcp.model;

public enum McpTransportType {
    STDIO("STDIO"),
    HTTP("HTTP");

    private final String displayName;

    McpTransportType(String displayName) {
        this.displayName = displayName;
    }

    public boolean isHttp() {
        return this == HTTP;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

