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
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.util.Dialogs;
import com.dbn.vector.model.request.EmbeddingChunkingConfig;
import com.dbn.vector.model.request.EmbeddingSourceType;
import com.dbn.vector.ui.VectorToolboxFormBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.Buttons.onButtonClick;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.util.Dialogs.whenOk;
import static com.dbn.vector.model.request.EmbeddingChunkingConfigValidator.validateMaxSize;
import static com.dbn.vector.model.request.EmbeddingChunkingConfigValidator.validateOverlap;

public class EmbeddingChunkingConfigForm extends VectorToolboxFormBase implements DBNCollapsibleForm {
    private JPanel mainPanel;
    private JComboBox<String> chunkByComboBox;
    private JComboBox<String> splitByComboBox;
    private JSpinner maxSizeSpinner;
    private JSpinner overlapSpinner;
    private JButton chunkLaboButton;
    private JLabel chunkByLabel;
    private JLabel splitByLabel;

    public EmbeddingChunkingConfigForm(@NotNull VectorToolboxFormBase parent) {
        super(parent);

        onButtonClick(chunkLaboButton, e -> openChunkLab());
        onSelectionChange(chunkByComboBox, e -> updateFieldAvailability());
    }

    private void openChunkLab() {
        EmbeddingChunkingConfig chunkConfig = new EmbeddingChunkingConfig();
        applyFormChanges(chunkConfig);

        Dialogs.show(() -> new EmbeddingChunkLabDialog(getConnection(), chunkConfig),
                whenOk(d -> resetFormChanges(d.getChunkConfig())));
    }

    private Integer getMaxSize() {
        return (Integer) maxSizeSpinner.getValue();
    }

    private Integer getOverlap() {
        return (Integer) overlapSpinner.getValue();
    }

    @Nullable
    private String getChunkBy() {
        return getSelection(chunkByComboBox);
    }

    @Nullable
    private String getSplitBy() {
        return getSelection(splitByComboBox);
    }

    @Override
    protected void initFieldAlignment() {
        FieldAlignerData alignerData = getFieldAlignerData();
        alignerData.registerFieldGroup(chunkByLabel/*, chunkByComboBox*/);
        alignerData.registerFieldGroup(splitByLabel/*, splitByComboBox*/);
    }

    @Override
    protected void initValidation() {
        addValidation(maxSizeSpinner, c -> validateMaxSize(getChunkBy(), getMaxSize()));
        addValidation(overlapSpinner, c -> validateOverlap(getMaxSize(), getOverlap()));
        addValidation(chunkByComboBox, c -> validateChunkBy());
    }

    private String validateChunkBy() {
        String chunkBy = getSelection(chunkByComboBox);
        if ("NONE".equals(chunkBy)) {
            EmbeddingSourceType sourceType = getToolboxForm().getEmbeddingSourceForm().getSelectedSourceType();
            if (sourceType == EmbeddingSourceType.FILE_SYSTEM) {
                return "Chunking configuration is mandatory for contents sourced from file system";
            }
        }

        return null;
    }

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsAvailability(() -> !"NONE".equals(getChunkBy()), array(
                splitByComboBox,
                maxSizeSpinner,
                overlapSpinner,
                chunkLaboButton));
    }

    @Override
    public void resetFormChanges() {
        EmbeddingChunkingConfig config = getConfig();
        resetFormChanges(config);
    }

    private void resetFormChanges(EmbeddingChunkingConfig config) {
        setSelection(chunkByComboBox, config.getChunkBy());
        setSelection(splitByComboBox, config.getSplitBy());
        maxSizeSpinner.setValue(config.getMaxSize());
        overlapSpinner.setValue(config.getOverlap());
    }

    @Override
    public void applyFormChanges() {
        EmbeddingChunkingConfig config = getConfig();
        applyFormChanges(config);
    }

    private void applyFormChanges(EmbeddingChunkingConfig config) {
        config.setChunkBy(getChunkBy());
        config.setSplitBy(getSplitBy());
        config.setMaxSize(getMaxSize());
        config.setOverlap(getOverlap());
    }

    public EmbeddingChunkingConfig getConfig() {
        return getEmbeddingRequest().getChunkConfig();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public String getFormTitle() {
        return "Chunk Configuration";
    }

    @Override
    public String getFormTitleDetail() {
        return getChunkBy() + " / " + getSplitBy() + " / " + maxSizeSpinner.getValue() + " / " + overlapSpinner.getValue();
    }
}
