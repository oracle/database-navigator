package com.dbn.mcp.build;

import com.dbn.common.ui.dialog.DBNDialog;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;

public class McpBuildResultDialog extends DBNDialog<McpBuildResultForm> {

    private final String configPath;
    private final String jarPath;
    private final String walletPath;
    private final String fullJson;
    private final String fragmentJson;

    public McpBuildResultDialog(@Nullable Project project,
                                 String configPath,
                                 String jarPath,
                                 String walletPath,
                                 String fullJson,
                                 String fragmentJson) {
        super(project, "MCP Build Complete", true);
        this.configPath = configPath;
        this.jarPath = jarPath;
        this.walletPath = walletPath;
        this.fullJson = fullJson;
        this.fragmentJson = fragmentJson;
        init();
    }

    @NotNull
    @Override
    protected McpBuildResultForm createForm() {
        return new McpBuildResultForm(this, configPath, jarPath, walletPath, fullJson, fragmentJson);
    }

    protected final Action[] initializeActions() {
        renameAction(getCancelAction(), "Close");
        return actions(getCancelAction());
    }
}
