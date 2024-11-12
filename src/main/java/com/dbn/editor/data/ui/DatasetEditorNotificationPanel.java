package com.dbn.editor.data.ui;

import com.dbn.common.editor.EditorNotificationPanel;
import com.dbn.common.message.MessageType;
import com.dbn.object.common.DBSchemaObject;

public abstract class DatasetEditorNotificationPanel extends EditorNotificationPanel {
    public DatasetEditorNotificationPanel(DBSchemaObject object, MessageType messageType) {
        super(object.getProject(), object.getVirtualFile(), messageType);
    }
}
