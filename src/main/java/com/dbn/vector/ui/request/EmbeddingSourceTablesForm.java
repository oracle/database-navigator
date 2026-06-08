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

package com.dbn.vector.ui.request;

import com.dbn.vector.model.request.EmbeddingSourceTables;
import com.dbn.vector.ui.VectorToolboxFormBase;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import static com.dbn.nls.NlsResources.txt;

public class EmbeddingSourceTablesForm extends VectorToolboxFormBase {
    private JPanel mainPanel;
    private JPanel tableListPanel;
    private JCheckBox autoSyncCheckBox;
    private JPanel autoSyncPanel;

    private EmbeddingSourceTablesListForm tableListForm;

    public EmbeddingSourceTablesForm(@NotNull VectorToolboxFormBase parent) {
        super(parent);
        initTableListForm();

        autoSyncPanel.setVisible(false); // TODO implement the "auto-sync" functionality or cleanup the ui
    }

    private void initTableListForm() {
        tableListForm = new EmbeddingSourceTablesListForm(this);
        tableListPanel.add(tableListForm.getComponent());
    }

    @Override
    protected void initValidation() {
        addValidation(
                tableListForm.getTableList(),
                list -> list.getModel().getSize() > 0,
                txt("msg.vector.error.SpecifyAtLeastOneTable")
        );
    }

    @Override
    public void resetFormChanges() {
        EmbeddingSourceTables config = getConfig();
        autoSyncCheckBox.setSelected(config.isAutoSync());
        tableListForm.setTables(config.getElements());
    }

    @Override
    public void applyFormChanges() {
        EmbeddingSourceTables config = getConfig();
        config.setAutoSync(autoSyncCheckBox.isSelected());
        config.setElements(tableListForm.getTables());
    }

    public EmbeddingSourceTables getConfig() {
        return getEmbeddingRequest().getSourceConfig().getSourceTables();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public int getTableCount() {
        return tableListForm.getTableList().getModel().getSize();
    }
}
