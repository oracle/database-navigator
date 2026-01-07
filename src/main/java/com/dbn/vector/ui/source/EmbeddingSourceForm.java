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
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.source.SourceConfig;
import com.dbn.vector.model.source.SourceType;
import com.dbn.vector.ui.VectorToolboxForm;
import com.dbn.vector.ui.VectorToolboxFormBase;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;

public class EmbeddingSourceForm extends VectorToolboxFormBase implements DBNCollapsibleForm {
  private JPanel mainPanel;
  private JPanel dataPanel;
  private JComboBox<SourceType> sourceComboBox;
  private JLabel sourceLabel;
  private EmbeddingSourceFilesForm fileSystemForm;
  private EmbeddingSourceTableForm tableForm;

  public EmbeddingSourceForm(@Nullable Disposable parent, ConnectionHandler connection) {
    super(parent, connection);
    initComboBox();
    initDataPanel();
  }

  private void initDataPanel() {
    ConnectionHandler connection = getConnection();
    fileSystemForm = new EmbeddingSourceFilesForm(this, connection);
    tableForm = new EmbeddingSourceTableForm(this, connection);
    updateSourceForm();
  }

  private void initComboBox() {
    ComboBoxes.initComboBox(sourceComboBox, SourceType.values());
    setSelection(sourceComboBox, SourceType.DATABASE_TABLE);
  }

  @Override
  protected void initEventListeners() {
    onSelectionChange(sourceComboBox, t -> updateSourceForm());
  }

  @Override
  protected void initFieldAlignment() {
    FieldAlignerData alignerData = getFieldAlignerData();
    alignerData.registerFieldGroup(sourceLabel, sourceComboBox);
    alignerData.registerForms(tableForm);
  }

  private void updateSourceForm() {
    SourceType sourceType = getSelectedSourceType();
    dataPanel.removeAll();
    if (sourceType == SourceType.FILE_SYSTEM) {
      dataPanel.add(fileSystemForm.getComponent());
    } else if (sourceType == SourceType.DATABASE_TABLE) {
      dataPanel.add(tableForm.getComponent());
    }
    dataPanel.revalidate();
    dataPanel.repaint();
    validateFormFields();

    VectorToolboxForm toolboxForm = getToolboxForm();
    toolboxForm.setStagingConfigVisible(sourceType == SourceType.FILE_SYSTEM);
  }

  @Override
  public void resetFormChanges() {
    SourceConfig config = getConfig();

    setSelection(sourceComboBox, config.getSourceType());
    tableForm.resetFormChanges();
    fileSystemForm.resetFormChanges();
  }

  @Override
  public void applyFormChanges() {
    SourceConfig config = getConfig();

    config.setSourceType(getSelectedSourceType());
    tableForm.applyFormChanges();
    fileSystemForm.applyFormChanges();
  }

  public SourceConfig getConfig() {
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
    SourceType sourceType = getSelectedSourceType();
    String sourceTypeName = sourceType == null ? "" : sourceType.getName();

    if (sourceType == SourceType.FILE_SYSTEM) {
      int count = fileSystemForm.getSelectedFileCount();
      return sourceTypeName + " - " + count + (count == 1 ? " file" : " files");
    }

    if (sourceType == SourceType.DATABASE_TABLE) {
      int count = tableForm.getSelectedTablesCount();
      return sourceTypeName + " - " + count + (count == 1 ? " table" : " tables");
    }
    return sourceTypeName;
  }

  public SourceType getSelectedSourceType() {
    return ComboBoxes.getSelection(sourceComboBox);
  }


}
