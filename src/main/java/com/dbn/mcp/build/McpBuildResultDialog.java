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
    private final String sourceProjectPath;
    private final boolean httpTransport;
    private final String claudeSnippetJson;
    private final String clineSnippetJson;

    public McpBuildResultDialog(@Nullable Project project,
                                 String configPath,
                                 String jarPath,
                                 String walletPath,
                                 String sourceProjectPath,
                                 boolean httpTransport,
                                 String claudeSnippetJson,
                                 String clineSnippetJson) {
        super(project, "MCP Build Complete", true);
        this.configPath = configPath;
        this.jarPath = jarPath;
        this.walletPath = walletPath;
        this.sourceProjectPath = sourceProjectPath;
        this.httpTransport = httpTransport;
        this.claudeSnippetJson = claudeSnippetJson;
        this.clineSnippetJson = clineSnippetJson;
        init();
    }

    @NotNull
    @Override
    protected McpBuildResultForm createForm() {
        return new McpBuildResultForm(this, configPath, jarPath, walletPath, sourceProjectPath, httpTransport,
                claudeSnippetJson, clineSnippetJson);
    }

    protected final Action[] initializeActions() {
        renameAction(getCancelAction(), "Close");
        return actions(getCancelAction());
    }
}
