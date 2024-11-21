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

package com.dbn.connection.config.file.ui;

import com.dbn.common.dispose.Failsafe;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.connection.config.file.DatabaseFileBundle;
import com.dbn.connection.config.ui.ConnectionUrlSettingsForm;
import com.intellij.ui.ToolbarDecorator;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;

import static com.dbn.common.ui.util.UserInterface.createToolbarDecorator;

public class DatabaseFileSettingsForm extends DBNFormBase {
    private JPanel mainPanel;
    private final DatabaseFilesTable table;

    public DatabaseFileSettingsForm(ConnectionUrlSettingsForm parent, DatabaseFileBundle fileBundle) {
        super(parent);
        table = new DatabaseFilesTable(this, fileBundle);

        ToolbarDecorator decorator = createToolbarDecorator(table);
        decorator.setAddAction(anActionButton -> getTable().insertRow());
        decorator.setRemoveAction(anActionButton -> getTable().removeRow());
        decorator.setRemoveActionUpdater(e -> getTable().getSelectedRows().length > 0);
        decorator.setMoveUpAction(anActionButton -> getTable().moveRowUp());
        decorator.setMoveUpActionUpdater(e -> getTable().getSelectedRow() > 1);
        decorator.setMoveDownAction(anActionButton -> getTable().moveRowDown());
        decorator.setMoveDownActionUpdater(e -> {
            int selectedRow = getTable().getSelectedRow();
            return selectedRow != 0 && selectedRow < getTable().getModel().getRowCount() -1;
        });
        decorator.setPreferredSize(new Dimension(-1, 300));
        JPanel panel = decorator.createPanel();
        mainPanel.add(panel, BorderLayout.CENTER);
        table.getParent().setBackground(table.getBackground());
    }
    
    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public DatabaseFilesTable getTable() {
        return Failsafe.nd(table);
    }

    public DatabaseFileBundle getFileBundle() {
        UserInterface.stopTableCellEditing(table);
        return table.getModel().getFileBundle();
    }

    public void setFileBundle(DatabaseFileBundle fileBundle) {
        table.getModel().setFileBundle(fileBundle);
    }
}
