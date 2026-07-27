package com.dbn.mcp.build;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionRef;
import com.dbn.mcp.deploy.McpGraalDeployDialog;
import com.dbn.mcp.model.McpServerDefinition;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;

import static com.dbn.nls.NlsResources.txt;

public class McpBuildResultDialog extends DBNDialog<McpBuildResultForm> {

    private final ConnectionRef connection;
    private final McpServerDefinition definition;
    private final McpBuilderResult result;

    public McpBuildResultDialog(
            @Nullable Project project,
            @NotNull ConnectionRef connection,
            @NotNull McpServerDefinition definition,
            @NotNull McpBuilderResult result) {
        super(project, txt("msg.mcp.title.McpBuildComplete"), true);
        this.connection = connection;
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

        // deployment targets a container image, so the action only applies to container builds
        if (!definition.getImplementation().isContainer()) {
            return actions(getCancelAction());
        }

        Action deployAction = createAction(txt("msg.mcp.button.DeployToGraal"), () -> openDeployDialog());
        return actions(deployAction, getCancelAction());
    }

    /**
     * Opens the deployment dialog without closing this one, so the user can return to the
     * build result (client snippets, run command, output paths) after deploying.
     */
    private void openDeployDialog() {
        Dialogs.show(() -> new McpGraalDeployDialog(getProject(), connection, definition, result));
    }
}
