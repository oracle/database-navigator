package com.dbn.mcp.util;

import com.dbn.common.util.Strings;
import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

import static com.dbn.nls.NlsResources.txt;

@UtilityClass
public class McpServerName {
    private static final int MAX_LENGTH = 63;
    private static final Pattern VALID_NAME = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]*$");

    public static String validationError(String value) {
        if (Strings.isEmptyOrSpaces(value)) {
            return txt("msg.mcp.error.ServerNameRequired");
        }
        if (value.length() > MAX_LENGTH) {
            return txt("msg.mcp.error.ServerNameTooLong", MAX_LENGTH);
        }
        if (".".equals(value) || "..".equals(value)) {
            return txt("msg.mcp.error.ServerNameInvalid");
        }
        if (value.contains("/") || value.contains("\\")) {
            return txt("msg.mcp.error.ServerNamePathSeparators");
        }
        if (!VALID_NAME.matcher(value).matches()) {
            return txt("msg.mcp.error.ServerNameCharactersInvalid");
        }

        return null;
    }
}
