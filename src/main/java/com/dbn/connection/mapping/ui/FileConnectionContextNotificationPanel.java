package com.dbn.connection.mapping.ui;

import com.dbn.common.editor.EditorNotificationPanel;
import com.dbn.common.message.MessageType;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.mapping.FileConnectionContext;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class FileConnectionContextNotificationPanel extends EditorNotificationPanel {

    public FileConnectionContextNotificationPanel(
            @NotNull Project project,
            @NotNull VirtualFile file,
            @NotNull FileConnectionContext mapping) {
        super(project, file ,MessageType.SYSTEM);

        ConnectionId connectionId = mapping.getConnectionId();
        ConnectionHandler connection = ConnectionHandler.get(connectionId);
        if (connection != null) {
            setText(connection.getName());
            setIcon(connection.getIcon());
        } else {
            setText("No connection selected");
            setIcon(null);
        }

        createActionLabel("Unlink", () -> delink());


/*
        Project project = editableObject.getProject();
        DBObjectRef<DBSchemaObject> editableObjectRef = DBObjectRef.of(editableObject);
        String objectName = editableObject.getQualifiedNameWithType();
        String objectTypeName = editableObject.getObjectType().getName();
        setText("This DDL file is attached to the database " + objectName + ". " +
                "Changes done to the " + objectTypeName + " are mirrored to this DDL file, overwriting any changes you may do to it.");
        createActionLabel("Detach", () -> {
            if (!project.isDisposed()) {
                DDLFileAttachmentManager attachmentManager = DDLFileAttachmentManager.getInstance(project);
                attachmentManager.detachDDLFile(virtualFile);
                DBSchemaObject editableObject1 = DBObjectRef.get(editableObjectRef);
                if (editableObject1 != null) {
                    DatabaseFileSystem.getInstance().reopenEditor(editableObject1);
                }
            }
        });
*/
    }

    private void delink() {
        Project project = getProject();
        VirtualFile file = getFile();
        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
        contextManager.removeMapping(file);
    }
}
