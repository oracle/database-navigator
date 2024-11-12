package com.dbn.assistant.editor.notification;

import com.dbn.assistant.DatabaseAssistantManager;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.state.AssistantStateListener;
import com.dbn.common.editor.EditorNotificationProvider;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.feature.FeatureAcknowledgement;
import com.dbn.common.util.Editors;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.dbn.language.sql.SQLFileType;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import static com.dbn.database.DatabaseFeature.AI_ASSISTANT;

public class AssistantEditorNotificationProvider extends EditorNotificationProvider<AssistantEditorNotificationPanel> {
    private static final Key<AssistantEditorNotificationPanel> KEY = Key.create("DBNavigator.AssistantEditorNotificationPanel");

    public AssistantEditorNotificationProvider() {
        ProjectEvents.subscribe(AssistantStateListener.TOPIC, assistantStateListener());
    }

    @NotNull
    private static AssistantStateListener assistantStateListener() {
        return (Project p, ConnectionId id) -> Editors.updateNotifications(p, null);
    }

    @NotNull
    @Override
    public Key<AssistantEditorNotificationPanel> getKey() {
        return KEY;
    }

    @Override
    public AssistantEditorNotificationPanel createComponent(@NotNull VirtualFile file, @NotNull FileEditor fileEditor, @NotNull Project project) {
        FileType fileType = file.getFileType();
        if (fileType != SQLFileType.INSTANCE) return null;

        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
        ConnectionHandler connection = contextManager.getConnection(file);
        if (connection == null) return null;
        if (connection.isVirtual()) return null;
        if (AI_ASSISTANT.isNotSupported(connection)) return null;

        DatabaseAssistantManager assistantManager = DatabaseAssistantManager.getInstance(project);
        AssistantState assistantState = assistantManager.getAssistantState(connection.getConnectionId());
        if (!assistantState.isSupported()) return null;

        if (assistantState.getAcknowledgement() != FeatureAcknowledgement.NONE) return null;

        return new AssistantEditorNotificationPanel(file, project);
    }
}
