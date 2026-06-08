package com.dbn.mcp.util;

import lombok.experimental.UtilityClass;

import static com.dbn.nls.NlsResources.txt;

@UtilityClass
public class McpToolDescription {
    public static String validationError(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean allowedControl = c == '\n' || c == '\r' || c == '\t';
            if (Character.isISOControl(c) && !allowedControl) {
                return txt("msg.mcp.error.DescriptionControlCharactersUnsupported");
            }
        }
        return null;
    }
}
