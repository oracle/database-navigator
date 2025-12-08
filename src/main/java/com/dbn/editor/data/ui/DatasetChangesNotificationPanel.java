/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.editor.data.ui;

import com.dbn.common.message.MessageType;
import com.dbn.editor.data.DataLoadInstructions;
import com.dbn.editor.data.DatasetEditor;
import com.dbn.event.notification.EventNotificationManager;
import com.dbn.object.DBTable;
import org.jetbrains.annotations.NotNull;

import static com.dbn.editor.data.DataLoadInstruction.DELIBERATE_ACTION;
import static com.dbn.editor.data.DataLoadInstruction.PRESERVE_CHANGES;
import static com.dbn.editor.data.DataLoadInstruction.USE_CURRENT_FILTER;

public class DatasetChangesNotificationPanel extends DatasetEditorNotificationPanel {
    private static final DataLoadInstructions RELOAD_INSTRUCTIONS = new DataLoadInstructions(USE_CURRENT_FILTER, PRESERVE_CHANGES, DELIBERATE_ACTION);

    public DatasetChangesNotificationPanel(DBTable table, @NotNull DatasetEditor editor) {
        super(table, editor, MessageType.WARNING);

        setText("The content of the " + table.getQualifiedNameWithType() + " has changed since the last time it was loaded.");
        createActionLabel("Reload data", () -> editor.loadData(RELOAD_INSTRUCTIONS));
        createActionLabel("Show events", () -> {
            EventNotificationManager eventNotificationManager = EventNotificationManager.getInstance(getProject());
            eventNotificationManager.showTableNotifications(table);

        });
    }
}
