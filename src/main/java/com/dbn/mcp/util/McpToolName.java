package com.dbn.mcp.util;

import com.dbn.common.util.Strings;
import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

@UtilityClass
public class McpToolName {
    private static final int MAX_LENGTH = 128;
    private static final Pattern VALID_NAME = Pattern.compile("^[A-Za-z0-9_.-]+$");

    public static String validationError(String value) {
        if (Strings.isEmptyOrSpaces(value)) {
            return "Please enter a tool name";
        }
        if (value.length() > MAX_LENGTH) {
            return "Tool name is too long (max " + MAX_LENGTH + " characters)";
        }
        if (value.contains(" ")) {
            return "No spaces are allowed in tool name";
        }
        if (!VALID_NAME.matcher(value).matches()) {
            return "Tool name can only contain letters, digits, '.', '-', and '_'";
        }

        return null;
    }
}
