package com.dbn.mcp.build;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.mcp.model.McpServerDefinition;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;

public class McpBuildResultDialog extends DBNDialog<McpBuildResultForm> {

    private final McpServerDefinition definition;
    private final McpBuilderResult result;

    public McpBuildResultDialog(
            @Nullable Project project,
            @NotNull McpServerDefinition definition,
            @NotNull McpBuilderResult result) {
        super(project, "MCP Build Complete", true);
        this.definition = definition;
        this.result = result;
        init();
    }

    @NotNull
    @Override
    protected McpBuildResultForm createForm() {
        return new McpBuildResultForm(this, definition, result);
    }

    protected final Action[] initializeActions() {
        renameAction(getCancelAction(), "Close");
        return actions(getCancelAction());
    }
}
