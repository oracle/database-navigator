package com.dbn.mcp.util;

import com.dbn.mcp.model.McpToolDefinition;
import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@UtilityClass
public class McpToolDefinitions {
    public static String validationError(String toolName, Collection<String> siblingToolNames) {
        String normalized = McpToolName.normalize(toolName);
        String error = McpToolName.validationError(normalized);
        if (error != null) {
            return error;
        }

        if (siblingToolNames != null) {
            for (String siblingName : siblingToolNames) {
                if (normalized.equals(McpToolName.normalize(siblingName))) {
                    return "Tool name is already in use";
                }
            }
        }

        return null;
    }

    public static String validationError(List<McpToolDefinition> tools) {
        if (tools == null || tools.isEmpty()) {
            return "Please add at least one tool";
        }

        Set<String> names = new LinkedHashSet<>();
        for (int i = 0; i < tools.size(); i++) {
            McpToolDefinition tool = tools.get(i);
            String name = tool == null ? "" : McpToolName.normalize(tool.getName());

            String error = McpToolName.validationError(name);
            if (error != null) {
                return "Tool #" + (i + 1) + ": " + error;
            }
            String description = tool == null ? "" : tool.getDescription();
            error = McpToolDescription.validationError(description);
            if (error != null) {
                return "Tool #" + (i + 1) + ": " + error;
            }
            if (!names.add(name)) {
                return "Tool names must be unique. Duplicate: " + name;
            }
        }
        return null;
    }
}
