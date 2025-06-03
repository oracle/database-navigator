package com.dbn.vector;

import com.dbn.assistant.DatabaseAssistantManager;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.ui.CardLayouts;
import com.dbn.common.ui.window.DBNToolWindowFactory;
import com.dbn.common.util.Editors;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.mapping.FileConnectionContextListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static com.dbn.common.icon.Icons.WINDOW_DATABASE_VECTOR;
import static com.dbn.common.util.ContextLookup.getConnectionId;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.vector.DatabaseVectorManager.TOOL_WINDOW_ID;

public class DatabaseVectorToolWindowFactory extends DBNToolWindowFactory {

    @Override
    protected void initialize(@NotNull ToolWindow toolWindow) {
        toolWindow.setTitle(txt("app.vector.title.DatabaseVector"));
        toolWindow.setStripeTitle(txt("app.vector.title.DatabaseVector"));
        toolWindow.setIcon(WINDOW_DATABASE_VECTOR.get());
    }

    @Override
    public void createContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        createContentPanel(toolWindow);
        toolWindow.setToHideOnEmptyContent(true);
        toolWindow.setAutoHide(false);

        DatabaseAssistantManager manager = DatabaseAssistantManager.getInstance(project);

        ProjectEvents.subscribe(project, manager,
                FileConnectionContextListener.TOPIC,
                createConnectionContextListener());

        ProjectEvents.subscribe(project, manager,
                ToolWindowManagerListener.TOPIC,
                createToolWindowListener(project));
    }

    private static void createContentPanel(@NotNull ToolWindow toolWindow) {
        ContentManager contentManager = toolWindow.getContentManager();
        JPanel contentPanel = CardLayouts.createCardPanel(true);

        ContentFactory contentFactory = contentManager.getFactory();
        Content content = contentFactory.createContent(contentPanel, null, true);
        contentManager.addContent(content);
    }

    private static @NotNull FileConnectionContextListener createConnectionContextListener() {
        return new FileConnectionContextListener() {
            @Override
            public void connectionChanged(Project project, VirtualFile file, ConnectionHandler connection) {
                if (!file.isInLocalFileSystem()) return; // changing connection in surrogate (LightVirtualFiles) should not cause connection switch

                ConnectionId connectionId = connection == null ? null : connection.getConnectionId();
                DatabaseAssistantManager manager = DatabaseAssistantManager.getInstance(project);
                manager.switchToConnection(connectionId);
            }
        };
    }

    private static ToolWindowManagerListener createToolWindowListener(Project project) {
        return new ToolWindowManagerListener() {

            @Override
            public void stateChanged(@NotNull ToolWindowManager toolWindowManager) {
                ToolWindow toolWindow = toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
                if (toolWindow == null) return;
                if (!toolWindow.isVisible()) return;

                VirtualFile file = Editors.getSelectedFile(project);
                ConnectionId connectionId = getConnectionId(project, file);
                if (connectionId == null) return; // do not switch away from last selected connection

                DatabaseAssistantManager assistantManager = DatabaseAssistantManager.getInstance(project);
                assistantManager.switchToConnection(connectionId);
            }
        };
    }

}
