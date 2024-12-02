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

package com.dbn.execution.statement.variables.ui;

import com.dbn.common.color.Colors;
import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.environment.EnvironmentType;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.ui.panel.DBNCollapsiblePanel;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.ComponentAligner;
import com.dbn.common.util.Documents;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Viewers;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.debugger.DBDebuggerType;
import com.dbn.execution.common.ui.ExecutionOptionsForm;
import com.dbn.execution.statement.StatementExecutionInput;
import com.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dbn.execution.statement.variables.StatementExecutionVariable;
import com.dbn.execution.statement.variables.StatementExecutionVariablesBundle;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.sql.SQLFileType;
import com.dbn.language.sql.SQLLanguage;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.ui.util.TextFields.onTextChange;

public class StatementExecutionInputForm extends DBNFormBase implements ComponentAligner.Container {
    private JPanel mainPanel;
    private JPanel variablesPanel;
    private JPanel executionOptionsPanel;
    private JPanel headerPanel;
    private JPanel debuggerVersionPanel;
    private JLabel debuggerVersionLabel;
    private JLabel debuggerTypeLabel;
    private JPanel previewPanel;
    private DBNScrollPane variablesScrollPane;

    private StatementExecutionProcessor executionProcessor;
    private final List<StatementExecutionVariableValueForm> variableValueForms = DisposableContainers.list(this);
    private final ExecutionOptionsForm executionOptionsForm;
    private final String statementText;
    private Document previewDocument;
    private EditorEx viewer;

    StatementExecutionInputForm(
            @NotNull StatementExecutionInputsDialog parent,
            @NotNull StatementExecutionProcessor executionProcessor,
            @NotNull DBDebuggerType debuggerType, boolean isBulkExecution) {
        super(parent);
        this.executionProcessor = executionProcessor;
        StatementExecutionInput executionInput = executionProcessor.getExecutionInput();
        this.statementText = executionInput.getExecutableStatementText();

        variablesPanel.setLayout(new BoxLayout(variablesPanel, BoxLayout.Y_AXIS));

        if (debuggerType.isDebug()) {
            debuggerVersionPanel.setVisible(true);
            debuggerTypeLabel.setText(debuggerType.name());
            debuggerVersionLabel.setText("...");

            Dispatch.async(
                    debuggerVersionLabel,
                    () -> executionInput.getDebuggerVersion(),
                    v -> debuggerVersionLabel.setText(v));
        } else {
            debuggerVersionPanel.setVisible(false);
        }

        DBLanguagePsiFile psiFile = executionProcessor.getPsiFile();
        String headerTitle = executionProcessor.getName();
        Icon headerIcon = executionProcessor.getIcon();
        Color headerBackground = psiFile == null ?
                EnvironmentType.DEFAULT.getColor() :
                psiFile.getEnvironmentType().getColor();

        DBNHeaderForm headerForm = new DBNHeaderForm(this, headerTitle, headerIcon, headerBackground);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

        StatementExecutionVariablesBundle executionVariables = executionProcessor.getExecutionVariables();
        if (executionVariables != null) {
            List<StatementExecutionVariable> variables = new ArrayList<>(executionVariables.getVariables());
            variables.sort(StatementExecutionVariablesBundle.NAME_COMPARATOR);


            for (StatementExecutionVariable variable: variables) {
                StatementExecutionVariableValueForm variableValueForm = new StatementExecutionVariableValueForm(this, variable);
                variableValueForms.add(variableValueForm);
                variablesPanel.add(variableValueForm.getComponent());

                onTextChange(variableValueForm.getEditorComponent(), e -> updatePreview());
            }
            Dimension preferredSize = variablesScrollPane.getPreferredSize();
            preferredSize.setSize(preferredSize.getWidth() + 20, preferredSize.getHeight());
            variablesScrollPane.setPreferredSize(preferredSize);

            ComponentAligner.alignFormComponents(this);
        }

        executionOptionsForm = new ExecutionOptionsForm(this, executionInput, debuggerType);
        DBNCollapsiblePanel collapsiblePanel = new DBNCollapsiblePanel(this, executionOptionsForm, false);
        executionOptionsPanel.add(collapsiblePanel.getComponent());
        //executionOptionsPanel.add(executionOptionsForm.getComponent());

        updatePreview();

        JCheckBox reuseVariablesCheckBox = executionOptionsForm.getReuseVariablesCheckBox();
        if (isBulkExecution && executionVariables != null) {
            reuseVariablesCheckBox.setVisible(true);
            reuseVariablesCheckBox.addActionListener(e -> getParentDialog().setReuseVariables(reuseVariablesCheckBox.isSelected()));
        } else {
            reuseVariablesCheckBox.setVisible(false);
        }
    }

    @Override
    public List<StatementExecutionVariableValueForm> getAlignableForms() {
        return variableValueForms;
    }

    @NotNull
    public StatementExecutionInputsDialog getParentDialog() {
        return ensureParentComponent();
    }

    public StatementExecutionProcessor getExecutionProcessor() {
        return executionProcessor;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        if (variableValueForms.isEmpty()) return null;

        return variableValueForms.get(0).getEditorComponent();
    }

    public void updateExecutionInput() {
        for (StatementExecutionVariableValueForm variableValueForm : variableValueForms) {
            variableValueForm.saveValue();
        }
        executionOptionsForm.updateExecutionInput();
    }

    void updatePreview() {
        ConnectionHandler connection = Failsafe.nn(executionProcessor.getConnection());
        SchemaId currentSchema = executionProcessor.getTargetSchema();
        Project project = connection.getProject();
        String previewText = this.statementText;

        StatementExecutionVariablesBundle executionVariables = executionProcessor.getExecutionVariables();
        if (executionVariables != null) {
            previewText = executionVariables.prepareStatementText(connection, this.statementText, true);

            for (StatementExecutionVariableValueForm variableValueForm : variableValueForms) {
                String errorText = executionVariables.getError(variableValueForm.getVariable());
                if (errorText == null)
                    variableValueForm.hideErrorLabel(); else
                    variableValueForm.showErrorLabel(errorText);
            }
        }


        if (previewDocument == null) {
            DBLanguageDialect languageDialect = connection.getLanguageDialect(SQLLanguage.INSTANCE);
            DBLanguagePsiFile selectStatementFile = DBLanguagePsiFile.createFromText(
                    project,
                    "preview.sql",
                    languageDialect,
                    previewText,
                    connection,
                    currentSchema);

            if (selectStatementFile == null) return;
            previewDocument = Documents.ensureDocument(selectStatementFile);

            viewer = Viewers.createViewer(previewDocument, project, null, SQLFileType.INSTANCE);
            viewer.setEmbeddedIntoDialogWrapper(true);
            Editors.initEditorHighlighter(viewer, SQLLanguage.INSTANCE, connection);
            Editors.setEditorReadonly(viewer, true);

            JScrollPane viewerScrollPane = viewer.getScrollPane();
            //viewerScrollPane.setBorder(null);
            viewerScrollPane.setViewportBorder(Borders.lineBorder(Colors.getReadonlyEditorBackground(), 4));

            EditorSettings settings = viewer.getSettings();
            settings.setFoldingOutlineShown(false);
            settings.setLineMarkerAreaShown(false);
            settings.setLineNumbersShown(false);
            settings.setVirtualSpace(false);
            settings.setDndEnabled(false);
            settings.setAdditionalLinesCount(2);
            settings.setRightMarginShown(false);
            JComponent viewerComponent = viewer.getComponent();
            previewPanel.add(viewerComponent, BorderLayout.CENTER);
        } else {
            Documents.setText(previewDocument, previewText);
        }
    }


    @Override
    public void disposeInner() {
        Editors.releaseEditor(viewer);
        executionProcessor = null;
        super.disposeInner();
    }
}
