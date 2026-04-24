package com.dbn.mcp;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.Components;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.mcp.build.McpMavenBuild;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.mcp.MCPServerManager.COMPONENT_NAME;

@State(
        name = COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class MCPServerManager extends ProjectComponentBase {
    public static final String COMPONENT_NAME = "DBNavigator.Project.MCPServerManager";

    public MCPServerManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
    }

    public static MCPServerManager getInstance(Project project) {
        return Components.projectService(project, MCPServerManager.class);
    }

    public void openMCPBuilder(@NotNull ConnectionHandler connection) {
        Project project = connection.getProject();
        if (!McpMavenBuild.isMavenPluginAvailable()) {
            int option = Messages.showConfirmationDialog(project,
                    "Maven Plugin Required",
                    "This feature requires the Maven plugin (org.jetbrains.idea.maven).\n" +
                            "Please enable or install it from IDE Plugins settings.",
                    new String[]{"Open Plugins", "Cancel"}, 0);
            if (option == 0) {
                McpMavenBuild.openMavenPluginSettings(project);
            }
            return;
        }

        if (!McpMavenBuild.isMavenAvailable(project)) {
            int option = Messages.showConfirmationDialog(project,
                    "Maven Required",
                    "Maven runtime is not available or invalid in IDE Maven settings.\n" +
                            "Please verify Maven settings and try again.",
                    new String[]{"Open Plugins", "Cancel"}, 0);
            if (option == 0) {
                McpMavenBuild.openMavenPluginSettings(project);
            }
            return;
        }

        Dialogs.show(() -> new McpServerInputDialog(connection));
    }
}
