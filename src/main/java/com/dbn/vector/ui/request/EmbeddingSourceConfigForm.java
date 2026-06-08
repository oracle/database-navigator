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

import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.vector.model.request.EmbeddingSourceConfig;
import com.dbn.vector.model.request.EmbeddingSourceType;
import com.dbn.vector.ui.VectorToolboxForm;
import com.dbn.vector.ui.VectorToolboxFormBase;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.nls.NlsResources.txt;

public class EmbeddingSourceConfigForm extends VectorToolboxFormBase implements DBNCollapsibleForm {
    private JPanel mainPanel;
    private JPanel dataPanel;
    private JComboBox<EmbeddingSourceType> sourceComboBox;
    private JLabel sourceLabel;
    private EmbeddingSourceFilesForm filesForm;
    private EmbeddingSourceTablesForm tablesForm;
    private EmbeddingSourceQueriesForm queriesForm;

    public EmbeddingSourceConfigForm(@NotNull VectorToolboxFormBase parent) {
        super(parent);
        initComboBox();
        initDataPanel();
    }

    private void initDataPanel() {
        filesForm = new EmbeddingSourceFilesForm(this);
        tablesForm = new EmbeddingSourceTablesForm(this);
        queriesForm = new EmbeddingSourceQueriesForm(this);
        updateSourceForm();
    }

    private void initComboBox() {
        ComboBoxes.initComboBox(sourceComboBox, EmbeddingSourceType.values());
        setSelection(sourceComboBox, EmbeddingSourceType.DATABASE_TABLE);
    }

    @Override
    protected void initEventListeners() {
        onSelectionChange(sourceComboBox, t -> updateSourceForm());
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerFieldGroup(sourceLabel, sourceComboBox);
    }

    private void updateSourceForm() {
        EmbeddingSourceType sourceType = getSelectedSourceType();
        dataPanel.removeAll();
        switch (sourceType) {
            case FILE_SYSTEM -> dataPanel.add(filesForm.getComponent());
            case DATABASE_TABLE -> dataPanel.add(tablesForm.getComponent());
            case DATABASE_QUERY -> dataPanel.add(queriesForm.getComponent());
        }
        dataPanel.revalidate();
        dataPanel.repaint();
        validateFormFields();

        VectorToolboxForm toolboxForm = getToolboxForm();
        toolboxForm.setStagingConfigVisible(sourceType == EmbeddingSourceType.FILE_SYSTEM);
    }

    @Override
    public void resetFormChanges() {
        EmbeddingSourceConfig config = getConfig();

        setSelection(sourceComboBox, config.getSourceType());
        queriesForm.resetFormChanges();
        tablesForm.resetFormChanges();
        filesForm.resetFormChanges();
    }

    @Override
    public void applyFormChanges() {
        EmbeddingSourceConfig config = getConfig();

        config.setSourceType(getSelectedSourceType());
        queriesForm.applyFormChanges();
        tablesForm.applyFormChanges();
        filesForm.applyFormChanges();
    }

    public EmbeddingSourceConfig getConfig() {
        return getEmbeddingRequest().getSourceConfig();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public String getFormTitle() {
        return txt("msg.vector.title.DataSource");
    }

    @Override
    public String getFormTitleDetail() {
        EmbeddingSourceType sourceType = getSelectedSourceType();
        String sourceTypeName = sourceType == null ? "" : sourceType.getName();

        if (sourceType == EmbeddingSourceType.FILE_SYSTEM) {
            int count = filesForm.getFileCount();
            return txt("msg.vector.text.SourceSelectionDetail", sourceTypeName, count, getFileUnit(count));
        }

        if (sourceType == EmbeddingSourceType.DATABASE_TABLE) {
            int count = tablesForm.getTableCount();
            return txt("msg.vector.text.SourceSelectionDetail", sourceTypeName, count, getTableUnit(count));
        }

        if (sourceType == EmbeddingSourceType.DATABASE_QUERY) {
            int count = queriesForm.getQueryCount();
            return txt("msg.vector.text.SourceSelectionDetail", sourceTypeName, count, getQueryUnit(count));
        }
        return sourceTypeName;
    }

    private String getFileUnit(int count) {
        return txt(count == 1 ? "msg.vector.unit.File" : "msg.vector.unit.Files");
    }

    private String getTableUnit(int count) {
        return txt(count == 1 ? "msg.vector.unit.Table" : "msg.vector.unit.Tables");
    }

    private String getQueryUnit(int count) {
        return txt(count == 1 ? "msg.vector.unit.Query" : "msg.vector.unit.Queries");
    }

    public EmbeddingSourceType getSelectedSourceType() {
        return ComboBoxes.getSelection(sourceComboBox);
    }


}
