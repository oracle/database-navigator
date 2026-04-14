package com.dbn.mcp.util;

import com.dbn.common.util.Strings;
import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

@UtilityClass
public class McpServerName {
    private static final int MAX_LENGTH = 63;
    private static final Pattern VALID_NAME = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]*$");

    public static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public static String validationError(String value) {
        String normalized = normalize(value);

        if (Strings.isEmptyOrSpaces(normalized)) {
            return "Please enter a server name";
        }
        if (normalized.length() > MAX_LENGTH) {
            return "Server name is too long (max " + MAX_LENGTH + " characters)";
        }
        if (".".equals(normalized) || "..".equals(normalized)) {
            return "Invalid server name";
        }
        if (normalized.contains("/") || normalized.contains("\\")) {
            return "Server name cannot contain path separators";
        }
        if (!VALID_NAME.matcher(normalized).matches()) {
            return "Use letters, digits, '.', '-', '_' and start with a letter or digit";
        }

        return null;
    }
}

