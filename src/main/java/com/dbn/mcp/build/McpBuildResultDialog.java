package com.dbn.mcp.build;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.mcp.model.McpServerDefinition;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;

import static com.dbn.nls.NlsResources.txt;

public class McpBuildResultDialog extends DBNDialog<McpBuildResultForm> {

    private final McpServerDefinition definition;
    private final McpBuilderResult result;

    public McpBuildResultDialog(
            @Nullable Project project,
            @NotNull McpServerDefinition definition,
            @NotNull McpBuilderResult result) {
        super(project, txt("msg.mcp.title.McpBuildComplete"), true);
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
        renameAction(getCancelAction(), txt("msg.shared.button.Close"));
        return actions(getCancelAction());
    }
}
