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

package com.dbn.vector.ui.source;

import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.source.DBTableSourceConfig;
import com.dbn.vector.ui.VectorToolboxFormBase;
import com.dbn.vector.ui.source.ui.TableListForm;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class EmbeddingSourceTableForm extends VectorToolboxFormBase {
    private JPanel mainPanel;
    private JPanel tableListPanel;
    private JCheckBox autoSyncCheckBox;

    private TableListForm tableListForm;

    public EmbeddingSourceTableForm(@Nullable Disposable parent, ConnectionHandler connection) {
        super(parent, connection);
        initTableListForm();
    }

    private void initTableListForm() {
        tableListForm = new TableListForm(this, "Source tables", getConnection());
        tableListPanel.add(tableListForm.getComponent(), BorderLayout.CENTER);
    }

    @Override
    protected void initValidation() {
        addValidation(
                tableListForm.getTableList(),
                list -> list.getModel().getSize() > 0,
                "Please select at least one table"
        );
    }

    @Override
    public void resetFormChanges() {
        DBTableSourceConfig config = getConfig();
        autoSyncCheckBox.setSelected(config.isAutoSync());
        tableListForm.setTables(config.getDbTableSources());
    }

    @Override
    public void applyFormChanges() {
        DBTableSourceConfig config = getConfig();
        config.setAutoSync(autoSyncCheckBox.isSelected());
        config.setDbTableSources(tableListForm.getTables());
    }

    public DBTableSourceConfig getConfig() {
        return getEmbeddingRequest().getSourceConfig().getTableSourceConfig();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
