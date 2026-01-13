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

public class EmbeddingSourceConfigForm extends VectorToolboxFormBase implements DBNCollapsibleForm {
  private JPanel mainPanel;
  private JPanel dataPanel;
  private JComboBox<EmbeddingSourceType> sourceComboBox;
  private JLabel sourceLabel;
  private EmbeddingSourceFilesForm filesForm;
  private EmbeddingSourceTablesForm tablesForm;

  public EmbeddingSourceConfigForm(@NotNull VectorToolboxFormBase parent) {
    super(parent);
    initComboBox();
    initDataPanel();
  }

  private void initDataPanel() {
    filesForm = new EmbeddingSourceFilesForm(this);
    tablesForm = new EmbeddingSourceTablesForm(this);
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
    alignerData.registerForms(tablesForm);
  }

  private void updateSourceForm() {
    EmbeddingSourceType sourceType = getSelectedSourceType();
    dataPanel.removeAll();
    if (sourceType == EmbeddingSourceType.FILE_SYSTEM) {
      dataPanel.add(filesForm.getComponent());
    } else if (sourceType == EmbeddingSourceType.DATABASE_TABLE) {
      dataPanel.add(tablesForm.getComponent());
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
    tablesForm.resetFormChanges();
    filesForm.resetFormChanges();
  }

  @Override
  public void applyFormChanges() {
    EmbeddingSourceConfig config = getConfig();

    config.setSourceType(getSelectedSourceType());
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
    return "Data Source";
  }

  @Override
  public String getFormTitleDetail() {
    EmbeddingSourceType sourceType = getSelectedSourceType();
    String sourceTypeName = sourceType == null ? "" : sourceType.getName();

    if (sourceType == EmbeddingSourceType.FILE_SYSTEM) {
      int count = filesForm.getSelectedFileCount();
      return sourceTypeName + " - " + count + (count == 1 ? " file" : " files");
    }

    if (sourceType == EmbeddingSourceType.DATABASE_TABLE) {
      int count = tablesForm.getSelectedTablesCount();
      return sourceTypeName + " - " + count + (count == 1 ? " table" : " tables");
    }
    return sourceTypeName;
  }

  public EmbeddingSourceType getSelectedSourceType() {
    return ComboBoxes.getSelection(sourceComboBox);
  }


}
