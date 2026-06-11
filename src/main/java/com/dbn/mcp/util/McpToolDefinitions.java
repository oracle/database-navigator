package com.dbn.mcp.util;

import com.dbn.mcp.model.McpToolDefinition;
import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.dbn.nls.NlsResources.txt;

@UtilityClass
public class McpToolDefinitions {
    public static String validationError(String toolName, Collection<String> siblingToolNames) {
        String error = McpToolName.validationError(toolName);
        if (error != null) return error;

        if (siblingToolNames == null) return null;


        for (String siblingToolName : siblingToolNames) {
            if (match(toolName, siblingToolName)) {
                return txt("msg.mcp.error.ToolNameAlreadyUsed");
            }
        }

        return null;
    }

    private static boolean match(String toolName, String siblingToolName) {
        // prevent tool definitions with same name but different separators
        toolName = toolName.replaceAll("[^A-Za-z0-9]", "");
        siblingToolName = siblingToolName.replaceAll("[^A-Za-z0-9]", "");
        return toolName.equalsIgnoreCase(siblingToolName);
    }

    public static String validationError(List<McpToolDefinition> tools) {
        if (tools == null || tools.isEmpty()) {
            return txt("msg.mcp.error.ToolRequired");
        }

        Set<String> names = new LinkedHashSet<>();
        for (int i = 0; i < tools.size(); i++) {
            McpToolDefinition tool = tools.get(i);
            String name = tool == null ? "" : tool.getName();

            String error = McpToolName.validationError(name);
            if (error != null) {
                return txt("msg.mcp.error.ToolValidationError", i + 1, error);
            }
            String description = tool == null ? "" : tool.getDescription();
            error = McpToolDescription.validationError(description);
            if (error != null) {
                return txt("msg.mcp.error.ToolValidationError", i + 1, error);
            }
            if (!names.add(name)) {
                return txt("msg.mcp.error.ToolNameDuplicate", name);
            }
        }
        return null;
    }
}
