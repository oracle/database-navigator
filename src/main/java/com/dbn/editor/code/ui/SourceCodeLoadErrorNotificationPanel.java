package com.dbn.editor.code.ui;

import com.dbn.common.message.MessageType;
import com.dbn.object.common.DBSchemaObject;

public class SourceCodeLoadErrorNotificationPanel extends SourceCodeEditorNotificationPanel{
    public SourceCodeLoadErrorNotificationPanel(DBSchemaObject object, String sourceLoadError) {
        super(object, MessageType.ERROR);
        setText("Could not load source for " + object.getQualifiedNameWithType() + ". Error details: " + sourceLoadError.replace("\n", " "));
    }
}
