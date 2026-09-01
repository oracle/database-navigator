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

package com.dbn.ml.ui;

import com.dbn.common.Priority;
import com.dbn.common.color.Colors;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.ui.misc.DBNTableScrollPane;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNResultSet;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseMachineLearningInterface;
import com.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dbn.data.model.resultSet.ResultSetDataModel;
import com.dbn.data.record.RecordViewInfo;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.sql.SQLFileType;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.ml.backend.model.MLPredictionAttribute;
import com.dbn.ml.model.MLTaskType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.util.Messages.showErrorDialog;
import static com.dbn.nls.NlsResources.txt;

/**
 * Form for entering feature values for ad-hoc prediction.
 *
 * @author ayoub allali
 */
public class MLPredictForm extends DBNFormBase {
    private static final int PREDICTION_ROW_LIMIT = 1;

    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel fieldsPanel;
    private DBNScrollPane fieldsScrollPane;
    private JPanel actionPanel;
    private JButton predictButton;
    private JPanel resultPanel;
    private DBNTableScrollPane resultScrollPane;
    private JPanel sqlPreviewPanel;

    private final String modelName;
    private final ConnectionHandler connection;
    private final MLTaskType taskType;
    private final List<MLPredictionAttribute> attributes;
    private final @NonNls String predictionStatement;
    private final List<JTextField> inputFields = new ArrayList<>();

    private EditorEx sqlPreviewViewer;
    private Document sqlPreviewDocument;
    private ResultSetTable resultTable;

    MLPredictForm(
            MLPredictDialog parent,
            String modelName,
            ConnectionHandler connection,
            MLTaskType taskType,
            List<MLPredictionAttribute> attributes,
            @NonNls String predictionStatement) {

        super(parent);
        this.modelName = modelName;
        this.connection = connection;
        this.taskType = taskType;
        this.attributes = new ArrayList<>(attributes);
        this.predictionStatement = predictionStatement;

        DBNHeaderForm headerForm = new DBNHeaderForm(this, connection);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

        buildFieldsPanel();
        initResultPanel();
        initPredictButton();
        whenFirstShown(this::initSqlPreview);
    }

    private void buildFieldsPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        for (int i = 0; i < attributes.size(); i++) {
            MLPredictionAttribute attribute = attributes.get(i);
            // Label
            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.fill = GridBagConstraints.NONE;
            gbc.weightx = 0;
            fieldsPanel.add(new JLabel(txt("app.machineLearning.label.FeatureInputType",
                    attribute.getName(), attribute.getDisplayDataType())), gbc);

            // Text field
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            JTextField field = new JTextField(20);
            field.setToolTipText(attribute.getDisplayDataType());
            onTextChange(field, e -> updateSqlPreview());
            inputFields.add(field);
            fieldsPanel.add(field, gbc);
        }
    }

    private void initResultPanel() {
        RecordViewInfo recordViewInfo = new RecordViewInfo(txt("app.machineLearning.title.Result"), null);
        ResultSetDataModel dataModel = new ResultSetDataModel<>(connection);
        resultTable = new ResultSetTable<>(this, dataModel, true, recordViewInfo);
        resultScrollPane.setViewportView(resultTable);
        resultTable.installValuePopupAddon();
        resultPanel.setBorder(Borders.lineBorder(Colors.getOutlineColor()));
        resultTable.setLoading(false);
    }

    private void initSqlPreview() {
        Project project = getProject();
        if (project == null) return;

        DBLanguageDialect languageDialect = connection.getLanguageDialect(SQLLanguage.INSTANCE);
        DBLanguagePsiFile previewFile = DBLanguagePsiFile.createFromText(
                project,
                "ml-prediction.sql",
                languageDialect,
                predictionStatement,
                connection,
                null);

        sqlPreviewDocument = previewFile == null ?
                Documents.createDocument(predictionStatement) :
                Documents.ensureDocument(previewFile);

        sqlPreviewViewer = Editors.createEditor(sqlPreviewDocument, project, null, SQLFileType.INSTANCE);
        sqlPreviewViewer.setEmbeddedIntoDialogWrapper(true);
        Editors.initEditorHighlighter(sqlPreviewViewer, SQLLanguage.INSTANCE, connection);
        Editors.setEditorReadonly(sqlPreviewViewer, true);

        EditorSettings settings = sqlPreviewViewer.getSettings();
        settings.setFoldingOutlineShown(false);
        settings.setLineMarkerAreaShown(false);
        settings.setLineNumbersShown(false);
        settings.setVirtualSpace(false);
        settings.setDndEnabled(false);
        settings.setAdditionalLinesCount(2);
        settings.setRightMarginShown(false);
        settings.setUseSoftWraps(true);
        settings.setCaretRowShown(false);

        Editors.updateEditorScrollPane(sqlPreviewViewer);
        Editors.installEditorLayoutUpdater(sqlPreviewViewer, this);
        sqlPreviewPanel.add(sqlPreviewViewer.getComponent());
        updateSqlPreview();
    }

    private void updateSqlPreview() {
        if (sqlPreviewDocument == null) return;

        StringBuilder preview = new StringBuilder(predictionStatement.length());
        int attributeIndex = 0;
        boolean quotedIdentifier = false;
        for (int index = 0; index < predictionStatement.length(); index++) {
            char character = predictionStatement.charAt(index);
            if (character == '"') {
                preview.append(character);
                if (quotedIdentifier && index + 1 < predictionStatement.length() &&
                        predictionStatement.charAt(index + 1) == '"') {
                    preview.append('"');
                    index++;
                } else {
                    quotedIdentifier = !quotedIdentifier;
                }
            } else if (character == '?' && !quotedIdentifier && attributeIndex < attributes.size()) {
                preview.append(toSqlLiteral(attributes.get(attributeIndex), inputFields.get(attributeIndex).getText().trim()));
                attributeIndex++;
            } else {
                preview.append(character);
            }
        }
        Documents.setText(sqlPreviewDocument, preview.toString());
    }

    private static String toSqlLiteral(MLPredictionAttribute attribute, String value) {
        try {
            return attribute.toSqlLiteral(value);
        } catch (IllegalArgumentException e) {
            return "NULL /* invalid input */";
        }
    }

    private void initPredictButton() {
        predictButton.addActionListener(e -> runPrediction());
    }

    private void runPrediction() {
        List<Object> values = new ArrayList<>();
        for (int i = 0; i < inputFields.size(); i++) {
            String value = inputFields.get(i).getText().trim();
            if (Strings.isEmpty(value)) {
                showError(txt("msg.machineLearning.error.FeatureValuesRequired"));
                return;
            }

            MLPredictionAttribute attribute = attributes.get(i);
            try {
                values.add(attribute.parseValue(value));
            } catch (IllegalArgumentException e) {
                showError(txt("msg.machineLearning.error.PredictionValueInvalid",
                        attribute.getName(), attribute.getDisplayDataType()));
                return;
            }
        }

        resultTable.setLoading(true);
        predictButton.setEnabled(false);

        Project project = connection.getProject();
        boolean isClassification = taskType == MLTaskType.CLASSIFICATION;

        Background.run(() -> {
            try {
                ResultSetDataModel result = executePrediction(project, values, isClassification);
                Dispatch.run(resultTable, () -> resultTable.setModel(result));
            } catch (Exception ex) {
                Dispatch.run(resultTable, () ->
                        showError(txt("msg.machineLearning.error.PredictionFailed", ex.getMessage())));
            } finally {
                Dispatch.run(resultTable, () -> {
                    resultTable.setLoading(false);
                    predictButton.setEnabled(true);
                });
            }
        });
    }

    private ResultSetDataModel executePrediction(
            Project project,
            List<Object> values,
            boolean withProbability) throws Exception {

        return DatabaseInterfaceInvoker.load(Priority.HIGH,
                "Predicting",
                "Running prediction",
                project,
                connection.getConnectionId(),
                conn -> {
                    DBNResultSet resultSet = null;
                    try {
                        DatabaseMachineLearningInterface mlInterface = connection.getInterfaces().getMachineLearningInterface();
                        resultSet = mlInterface.predict(conn, modelName, attributes, values, withProbability);

                        ResultSetDataModel dataModel = new ResultSetDataModel(resultSet, connection, -1);
                        dataModel.fetchNextRecords(PREDICTION_ROW_LIMIT, false);
                        return dataModel;
                    } finally {
                        Resources.close(resultSet);
                    }
                });
    }

    private void showError(String message) {
        showErrorDialog(getProject(), txt("msg.machineLearning.title.AdHocPrediction"), message);
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return inputFields.isEmpty() ? null : inputFields.get(0);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public void disposeInner() {
        Editors.releaseEditor(sqlPreviewViewer);
        sqlPreviewViewer = null;
        sqlPreviewDocument = null;
        super.disposeInner();
    }
}
