package com.dbn.assistant.editor.notification;

import com.dbn.assistant.DatabaseAssistantManager;
import com.dbn.assistant.state.AssistantState;
import com.dbn.common.editor.EditorNotificationPanel;
import com.dbn.common.feature.FeatureAcknowledgement;
import com.dbn.common.icon.Icons;
import com.dbn.common.message.MessageType;
import com.dbn.common.ui.HyperLinkForm;
import com.dbn.connection.ConnectionId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class AssistantEditorNotificationPanel extends EditorNotificationPanel {

    public AssistantEditorNotificationPanel(@NotNull VirtualFile file,  @NotNull Project project) {
        super(project, file, MessageType.INFO);
        setIcon(Icons.COMMON_INFO);
        HyperLinkForm hyperLinkForm = HyperLinkForm.create(
                "Your database supports natural language interaction powered by",
                "Oracle Select AI",
                "https://www.oracle.com/autonomous-database/select-ai/");
        setContent(hyperLinkForm);

        createActionLabel("Configure", () -> configure());
        createActionLabel("Chat", () -> chat());
        createActionLabel("Dismiss", () -> dismiss());
    }

    private void configure() {
        ConnectionId connectionId = getConnectionId();
        DatabaseAssistantManager assistantManager = getAssistantManager();
        assistantManager.openProfileConfiguration(connectionId);
    }

    private void chat() {
        ConnectionId connectionId = getConnectionId();
        DatabaseAssistantManager assistantManager = getAssistantManager();
        AssistantState assistantState = assistantManager.getAssistantState(connectionId);
        assistantState.setAcknowledgement(FeatureAcknowledgement.NOTICED);
        assistantManager.showToolWindow(connectionId);
    }

    private void dismiss() {
        ConnectionId connectionId = getConnectionId();
        DatabaseAssistantManager assistantManager = getAssistantManager();
        AssistantState assistantState = assistantManager.getAssistantState(connectionId);
        assistantState.setAcknowledgement(FeatureAcknowledgement.DISMISSED);
    }

    private @NotNull DatabaseAssistantManager getAssistantManager() {
        return DatabaseAssistantManager.getInstance(getProject());
    }

}
