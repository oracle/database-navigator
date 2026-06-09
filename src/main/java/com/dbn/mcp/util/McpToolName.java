package com.dbn.mcp.util;

import com.dbn.common.util.Strings;
import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

import static com.dbn.nls.NlsResources.txt;

@UtilityClass
public class McpToolName {
    private static final int MAX_LENGTH = 128;
    private static final Pattern VALID_NAME = Pattern.compile("^[A-Za-z0-9_.-]+$");

    public static String validationError(String value) {
        if (Strings.isEmptyOrSpaces(value)) {
            return txt("msg.mcp.error.ToolNameRequired");
        }
        if (value.length() > MAX_LENGTH) {
            return txt("msg.mcp.error.ToolNameTooLong", MAX_LENGTH);
        }
        if (value.contains(" ")) {
            return txt("msg.mcp.error.ToolNameSpacesUnsupported");
        }
        if (!VALID_NAME.matcher(value).matches()) {
            return txt("msg.mcp.error.ToolNameCharactersInvalid");
        }

        return null;
    }
}
