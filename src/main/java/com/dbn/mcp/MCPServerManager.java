package com.dbn.mcp;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.Components;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.util.Dialogs;
import com.dbn.event.notification.EventNotificationManager;
import com.dbn.event.registration.EventRegistrationManager;
import com.dbn.event.registration.ui.EventRegistrationInputDialog;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

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

  public static MCPServerManager getInstance(Project project) throws MavenInvocationException, IOException {
    return Components.projectService(project, MCPServerManager.class);
  }


  public void showMCPManager() {
    Dialogs.show(() -> new McpServerInputDialog(getProject()));
  }
}
