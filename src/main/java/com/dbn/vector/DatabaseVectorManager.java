package com.dbn.vector;

import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.connection.ConnectionId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;

import static com.dbn.common.ui.CardLayouts.isBlankCard;
import static com.dbn.common.ui.CardLayouts.visibleCardId;

public abstract class DatabaseVectorManager extends ProjectComponentBase implements PersistentState {
    public static final String TOOL_WINDOW_ID = "DB VECTOR";

    protected DatabaseVectorManager(@NotNull Project project, String componentName) {
        super(project, componentName);
    }

    /**
     * switch from current connection to the new selected one from DBN navigator
     *
     * @param connectionId the new selected connection
     */
    public void switchToConnection(@Nullable ConnectionId connectionId) {
        JPanel toolWindowPanel = getToolWindowPanel();
        if (toolWindowPanel == null) return;

        String id = visibleCardId(toolWindowPanel);
        ConnectionId selectedConnectionId = isBlankCard(id) ? null : ConnectionId.get(id);

        if (Objects.equals(selectedConnectionId, connectionId)) return;
//        initToolWindow(connectionId);
    }

    @Nullable
    private JPanel getToolWindowPanel() {
        ToolWindow toolWindow = getToolWindow();
        if (toolWindow == null) return null;

        Content content = toolWindow.getContentManager().getContent(0);
        return content == null ? null : (JPanel) content.getComponent();
    }

    @Nullable
    public ToolWindow getToolWindow() {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(getProject());
        return toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
    }

}
