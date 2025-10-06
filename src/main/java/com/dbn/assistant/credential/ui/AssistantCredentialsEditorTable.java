/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.assistant.credential.ui;

import com.dbn.assistant.credential.AssistantCredentialBundle;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.table.DBNEntityEditableTable;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.ListSelectionModel;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;

/**
 * Table model for provider credentials information
 * The template credential stored locally.
 */
public class AssistantCredentialsEditorTable extends DBNEntityEditableTable<AssistantCredentialsTableModel> {

    AssistantCredentialsEditorTable(DBNComponent parent, AssistantCredentialBundle credentials) {
        super(parent, createModel(credentials), true);
        setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        setSelectionBackground(UIUtil.getTableBackground());
        setSelectionForeground(UIUtil.getTableForeground());
        setCellSelectionEnabled(true);
        setDefaultRenderer(String.class, new AssistantCredentialsTableCellRenderer());

        setAccessibleName(this, "Assistant Credentials");
        setProportionalColumnWidths(15, 15, 15, 55);
    }

    @NotNull
    private static AssistantCredentialsTableModel createModel(AssistantCredentialBundle credentials) {
        return new AssistantCredentialsTableModel(credentials);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false; //getModel().isCellEditable(row, column);
    }
}
