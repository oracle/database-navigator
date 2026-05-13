package com.dbn.mcp.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class McpToolDescription {
    public static String validationError(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean allowedControl = c == '\n' || c == '\r' || c == '\t';
            if (Character.isISOControl(c) && !allowedControl) {
                return "Description contains unsupported control characters";
            }
        }
        return null;
    }
}

