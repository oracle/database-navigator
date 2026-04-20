package com.dbn.mcp.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class McpToolDescription {
    public static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public static String validationError(String value) {
        String normalized = normalize(value);
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            boolean allowedControl = c == '\n' || c == '\r' || c == '\t';
            if (Character.isISOControl(c) && !allowedControl) {
                return "Description contains unsupported control characters";
            }
        }
        return null;
    }
}

