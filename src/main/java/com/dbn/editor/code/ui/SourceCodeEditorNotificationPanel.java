package com.dbn.editor.code.ui;

import com.dbn.common.editor.EditorNotificationPanel;
import com.dbn.common.message.MessageType;
import com.dbn.object.common.DBSchemaObject;

public abstract class SourceCodeEditorNotificationPanel extends EditorNotificationPanel {
    public SourceCodeEditorNotificationPanel(DBSchemaObject object, MessageType messageType) {
        super(object.getProject(), object.getVirtualFile(), messageType);
    }
}
