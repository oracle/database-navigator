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

package com.dbn.vector.ui.result;

import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.info.DBNInfoLabel;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.ui.text.HiddenCaret;
import com.dbn.common.ui.util.Fonts;
import com.dbn.connection.ConnectionHandler;
import com.dbn.editor.DatabaseFileEditorManager;
import com.dbn.editor.data.DatasetEditorManager;
import com.dbn.editor.data.filter.DatasetCustomFilter;
import com.dbn.editor.data.filter.DatasetFilterGroup;
import com.dbn.editor.data.filter.DatasetFilterManager;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.vector.model.result.EmbeddingFileResult;
import com.dbn.vector.model.result.EmbeddingResult;
import com.dbn.vector.model.result.EmbeddingTableResult;
import com.dbn.vector.model.result.PipelineStep;
import com.dbn.vector.model.result.StepResult;
import com.intellij.icons.AllIcons;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.Color;
import java.awt.Font;

public class PipelineStepForm extends DBNFormBase  {
  private static final String TABLE_NAME_SEPARATOR = "\\.";
  private static final String FILTER_NAME_PREFIX = "New Embeddings - ";
  private static final String SQL_QUOTE = "'";
  private static final String SQL_ESCAPED_QUOTE = "''";// for validating sql condition

  private JPanel mainPanel;
  private JLabel titleLabel;
  private JTextPane reasonTextArea;
  private JPanel statusPanel;
  private JLabel statusLabel;
  private JLabel durationValueLabel;
  private DBNInfoLabel infoLabel;
  private DBNHyperlinkLabel linkLabel;

  private final StepResult stepResult;

  public PipelineStepForm(@Nullable VectorEmbeddingExecutionResultForm parent, StepResult stepResult, int order) {
    super(parent);
    this.stepResult = stepResult;
    Color greyContent = Colors.faded(UIUtil.getLabelForeground());
    Color redContent = UIUtil.getErrorForeground();
    Font largerFont = Fonts.regular(1);
    titleLabel.setText(order+" "+stepResult.getStep().getDisplayName());
    titleLabel.setFont(largerFont);
    statusLabel.setForeground(greyContent);
    initInfoLabel();



    linkLabel.setHyperlinkText(stepResult.getLink());
    linkLabel.setIcon(stepResult.getIcon());

    linkLabel.addHyperlinkListener(e -> {handleTableLinkClick();});






    durationValueLabel.setForeground(greyContent);
    durationValueLabel.setText(String.format("%.1f",(double)stepResult.getDuration()/1000)+"s");
    if (!stepResult.isOk()){
      reasonTextArea.setText(stepResult.getErrorCode()+stepResult.getErrorMessage());
      reasonTextArea.setFont(JBUI.Fonts.label());
      reasonTextArea.setForeground(redContent);
      reasonTextArea.setCaret(new HiddenCaret());
    }
    reasonTextArea.setVisible(!stepResult.isOk());
    linkLabel.setVisible(!stepResult.getLink().isEmpty());
    updateStepStatus();


  }

  private void handleTableLinkClick() {
    VectorEmbeddingExecutionResultForm parentForm = getParentComponent();
    if (parentForm == null) return;

    ConnectionHandler connection = parentForm.getResult().getConnection();
    DBTable table = findTable(connection, stepResult.getLink());
    if (table == null) return;

    EmbeddingResult selectedSource = parentForm.getSelectedSource();
    boolean shouldFilter = stepResult.getStep() == PipelineStep.ENSURE_DESTINATION
            && selectedSource != null;

    openTableEditor(connection, table, selectedSource, shouldFilter);
  }

  @Nullable
  private DBTable findTable(ConnectionHandler connection, String fullTableName) {
    String[] parts = fullTableName.split(TABLE_NAME_SEPARATOR);
    if (parts.length != 2) return null;

    String schemaName = parts[0];
    String tableName = parts[1];

    DBSchema schema = connection.getSchema(connection.getSchemaId(schemaName));
    if (schema == null) return null;

    return schema.getTable(tableName);
  }

  private void openTableEditor(
          ConnectionHandler connection,
          DBTable table,
          @Nullable EmbeddingResult selectedSource,
          boolean applyFilter) {
    DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(connection.getProject());
    boolean editorAlreadyOpen = editorManager.isFileOpen(table);

    if (applyFilter && selectedSource != null) {
      createAndApplyFilter(connection, table, selectedSource);
    }

    editorManager.connectAndOpenEditor(table, null, true, true);

    if (editorAlreadyOpen) {
      reloadEditorData(connection, table);
    }
  }

  private void reloadEditorData(ConnectionHandler connection, DBTable table) {
    DatasetEditorManager datasetEditorManager = DatasetEditorManager.getInstance(connection.getProject());
    datasetEditorManager.reloadEditorData(table);
  }

  private void createAndApplyFilter(ConnectionHandler connection, DBTable table, EmbeddingResult embeddingResult) {
    DatasetFilterManager filterManager = DatasetFilterManager.getInstance(connection.getProject());
    DatasetFilterGroup filterGroup = filterManager.getFilterGroup(table);

    DatasetCustomFilter filter = createTemporaryFilter(filterGroup, embeddingResult);
    String whereClause = buildWhereClause(embeddingResult);

    if (whereClause == null) return;

    filter.setCondition(whereClause);
    filterManager.setActiveFilter(table, filter);
  }

  private DatasetCustomFilter createTemporaryFilter(DatasetFilterGroup filterGroup, EmbeddingResult embeddingResult) {
    DatasetCustomFilter filter = new DatasetCustomFilter(
            filterGroup,
            FILTER_NAME_PREFIX + embeddingResult.getName()
    );
    filter.setTemporary(true);
    return filter;
  }

  @Nullable
  private String buildWhereClause(EmbeddingResult embeddingResult) {
    if (embeddingResult instanceof EmbeddingTableResult) {
      return buildTableWhereClause((EmbeddingTableResult) embeddingResult);
    } else if (embeddingResult instanceof EmbeddingFileResult) {
      return buildFileWhereClause((EmbeddingFileResult) embeddingResult);
    }
    return null;
  }

  @Nullable
  private String buildTableWhereClause(EmbeddingTableResult tableResult) {
    String tableName = extractTableName(tableResult.getIdentifier());
    if (tableName == null) return null;

    return String.format(
            "JSON_VALUE(metadata, '$.embedding_source.table_name') = '%s'",
            escapeSql(tableName)
    );
  }

  private String buildFileWhereClause(EmbeddingFileResult fileResult) {
    String fileStoreId = fileResult.getFileStoreId();
    return String.format(
            "JSON_VALUE(metadata, '$.embedding_source.source_id') = '%s'",
            escapeSql(fileStoreId)
    );
  }

  @Nullable
  private String extractTableName(String identifier) {
    String[] parts = identifier.split(TABLE_NAME_SEPARATOR);
    return parts.length == 2 ? parts[1] : null;
  }

  private String escapeSql(String value) {
    return value.replace(SQL_QUOTE, SQL_ESCAPED_QUOTE);
  }

  private void initInfoLabel() {
    infoLabel.setContent(TextContent.plain(stepResult.getStep().getDescription()));
  }

  private void updateStepStatus() {
    StepResult.STEP_STATUS status = stepResult.getStatus();
    Icon icon = null;
    if (status == StepResult.STEP_STATUS.FAILED) {
      statusLabel.setText("Not OK");
      icon = Icons.COMMON_STATUS_ERROR;
      statusLabel.setIcon(icon);
    }else if (status == StepResult.STEP_STATUS.SUCCEEDED){
      statusLabel.setText("OK");
      icon = Icons.COMMON_STATUS_SUCCESS;
      statusLabel.setIcon(icon);
    }else if (status == StepResult.STEP_STATUS.NOT_STARTED){
      statusLabel.setText("Not Started");
      icon = AllIcons.Process.Step_passive;
      statusLabel.setIcon(icon);
    }

  }


  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }
}
