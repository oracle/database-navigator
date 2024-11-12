package com.dbn.editor.data.ui;

import com.dbn.common.message.MessageType;
import com.dbn.object.common.DBSchemaObject;

public class DatasetEditorLoadErrorNotificationPanel extends DatasetEditorNotificationPanel {
    public DatasetEditorLoadErrorNotificationPanel(DBSchemaObject object, String sourceLoadError) {
        super(object, MessageType.ERROR);
        setText("Could not load data for " + object.getQualifiedNameWithType() + ". Error details: " + sourceLoadError.replace("\n", " "));
    }
}
