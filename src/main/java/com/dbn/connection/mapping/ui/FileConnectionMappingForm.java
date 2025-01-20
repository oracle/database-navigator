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

package com.dbn.connection.mapping.ui;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.connection.mapping.FileConnectionContext;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.List;

public class FileConnectionMappingForm extends DBNFormBase {
    private JBScrollPane mappingsTableScrollPane;
    private JPanel mainPanel;

    private final DBNTable<FileConnectionMappingTableModel> mappingsTable;

    public FileConnectionMappingForm(@Nullable Disposable parent) {
        super(parent);
        Project project = ensureProject();
        FileConnectionContextManager manager = FileConnectionContextManager.getInstance(project);
        List<FileConnectionContext> mappings = new ArrayList<>(manager.getRegistry().getMappings().values());
        FileConnectionMappingTableModel model = new FileConnectionMappingTableModel(mappings);
        mappingsTable = new FileConnectionMappingTable(this, model);

        mappingsTable.adjustColumnWidths();
        mappingsTableScrollPane.setViewportView(mappingsTable);

    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return mappingsTable;
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
