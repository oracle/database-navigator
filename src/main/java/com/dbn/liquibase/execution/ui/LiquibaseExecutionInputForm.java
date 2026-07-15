/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.liquibase.execution.ui;

import com.dbn.common.locale.Formatter;
import com.dbn.common.routine.Consumer;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.ValueFactory;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.form.field.FieldState;
import com.dbn.common.ui.info.DBNCommentLabel;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.DatabaseType;
import com.dbn.data.editor.ui.TextFieldWithPopup;
import com.dbn.liquibase.DatabaseLiquibaseManager;
import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.execution.LiquibaseRollbackType;
import com.dbn.liquibase.model.LiquibaseWorkspace;
import com.dbn.liquibase.model.LiquibaseWorkspaceBundle;
import com.dbn.liquibase.model.LiquibaseWorkspacePaths;
import com.dbn.object.DBSchema;
import com.dbn.object.common.ui.DBObjectSelector;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import static com.dbn.common.text.TextContent.plain;
import static com.dbn.common.ui.form.field.DBNFormFieldDisabler.setFormFieldEnabled;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.liquibase.execution.LiquibaseOperation.ROLLBACK;
import static com.dbn.liquibase.execution.LiquibaseRollbackType.COUNT;
import static com.dbn.liquibase.execution.LiquibaseRollbackType.DATE;
import static com.dbn.liquibase.execution.LiquibaseRollbackType.TAG;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.type.DBObjectType.SCHEMA;
import static java.util.Collections.emptyList;

public class LiquibaseExecutionInputForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel rollbackDateFieldPanel;
    private JLabel sourceConnectionLabel;
    private JLabel sourceSchemaLabel;
    private JLabel targetConnectionLabel;
    private JLabel targetSchemaLabel;
    private JLabel rollbackTypeLabel;
    private JLabel rollbackTagLabel;
    private JLabel rollbackDateLabel;
    private JLabel rollbackCountLabel;
    private JSpinner rollbackCountSpinner;
    private JTextField rollbackTagTextField;
    private DBNComboBox<ConnectionHandler> sourceConnectionSelector;
    private DBNComboBox<ConnectionHandler> targetConnectionSelector;
    private DBNComboBox<LiquibaseWorkspace> workspaceSelector;
    private DBNComboBox<LiquibaseRollbackType> rollbackTypeSelector;
    private DBNCommentLabel workspacePathLabel;
    private DBNCommentLabel rollbackCountInfoLabel;
    private DBNCommentLabel rollbackTagInfoLabel;
    private DBNCommentLabel rollbackDateInfoLabel;
    private TextFieldWithPopup<?> rollbackDateField;
    private DBObjectSelector<DBSchema> sourceSchemaSelector;
    private DBObjectSelector<DBSchema> targetSchemaSelector;

    private final LiquibaseExecutionInputDialog parent;
    private final LiquibaseExecutionInput executionInput;

    LiquibaseExecutionInputForm(@NotNull LiquibaseExecutionInputDialog parent) {
        super(parent);
        this.parent = parent;
        this.executionInput = parent.getExecutionInput();

        initHeaderPanel();
        initHintPanel();
        initContextLabels();
        initWorkspaceSelector();
        initRollbackFields();
        initSourceContextSelectors();
        initTargetContextSelectors();
        executionInput.setWorkspace(workspaceSelector.getSelectedValue());
    }

    private void initRollbackFields() {
        rollbackDateField = new TextFieldWithPopup<>(executionInput.getProject());
        rollbackDateFieldPanel.add(rollbackDateField);

        boolean visible = executionInput.getOperation() == ROLLBACK;
        if (!visible) return;

        rollbackDateField.createCalendarPopup(false);
        rollbackTypeSelector.setValues(List.of(LiquibaseRollbackType.values()));
        rollbackTypeSelector.setSelectedValue(executionInput.getRollbackType());
        rollbackCountLabel.setVisible(visible);
        rollbackCountSpinner.setVisible(visible);
        rollbackCountSpinner.setModel(new SpinnerNumberModel(executionInput.getRollbackCount(), 1, Integer.MAX_VALUE, 1));
        rollbackTagTextField.setText(executionInput.getRollbackTag());
        setText(rollbackDateField.getTextField(), executionInput.getRollbackDate());
        onSelectionChange(rollbackTypeSelector, value -> {
            updateFieldAvailability();
            markFormChanged();
        });
    }

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsVisibility(() -> isRollbackOperation(), array(rollbackTypeLabel, rollbackTypeSelector));
        fieldAdapter.initFieldsVisibility(() -> isRollbackType(COUNT), array(
                rollbackCountLabel,
                rollbackCountSpinner,
                rollbackCountInfoLabel));

        fieldAdapter.initFieldsVisibility(() -> isRollbackType(TAG), array(
                rollbackTagLabel,
                rollbackTagTextField,
                rollbackTagInfoLabel));

        fieldAdapter.initFieldsVisibility(() -> isRollbackType(DATE), array(
                rollbackDateLabel,
                rollbackDateFieldPanel,
                rollbackDateInfoLabel));
    }

    private boolean isRollbackOperation() {
        return executionInput.getOperation() == ROLLBACK;
    }

    private boolean isRollbackType(@NotNull LiquibaseRollbackType type) {
        return isRollbackOperation() && getSelection(rollbackTypeSelector) == type;
    }

    private void initHeaderPanel() {
        ConnectionHandler connection = executionInput.getRelevantConnection();
        headerPanel.add(new DBNHeaderForm(this, connection).getComponent());
    }

    private void initHintPanel() {
        LiquibaseOperation operation = executionInput.getOperation();
        TextContent hint = plain(operation.getHint());
        hintPanel.add(new DBNHintForm(this, hint, null, true).getComponent());
    }

    private void initContextLabels() {
        LiquibaseOperation operation = executionInput.getOperation();
        boolean sourceVisible = operation.getSourceContextState().isVisible();
        boolean targetVisible = operation.getTargetContextState().isVisible();
        boolean qualified = sourceVisible && targetVisible;

        sourceConnectionLabel.setText(txt(qualified ?
                "cfg.liquibase.label.SourceConnection" :
                "app.object.label.Connection"));
        sourceSchemaLabel.setText(txt(qualified ?
                "cfg.liquibase.label.SourceSchema" :
                "app.object.label.Schema"));
        targetConnectionLabel.setText(txt(qualified ?
                "cfg.liquibase.label.TargetConnection" :
                "app.object.label.Connection"));
        targetSchemaLabel.setText(txt(qualified ?
                "cfg.liquibase.label.TargetSchema" :
                "app.object.label.Schema"));
    }

    private void initWorkspaceSelector() {
        LiquibaseWorkspaceBundle workspaces = parent.getWorkspaces();
        ConnectionHandler connection = executionInput.getRelevantConnection();
        DBSchema schema = executionInput.getRelevantSchema();

        DatabaseType databaseType = connection.getDatabaseType();
        List<LiquibaseWorkspace> availableWorkspaces = workspaces.getWorkspaces(databaseType);

        LiquibaseWorkspace selectedWorkspace = workspaces.getSelectedWorkspace(
                schema.getConnectionId(),
                schema.getSchemaId());
        workspaceSelector.setValues(availableWorkspaces);
        workspaceSelector.setSelectedValue(availableWorkspaces.contains(selectedWorkspace) ? selectedWorkspace : null);
        updateWorkspacePath();
        workspaceSelector.withValueFactory(new ValueFactory<>(txt("app.liquibase.action.NewWorkspace")) {
            @Override
            public void createValue(Consumer<LiquibaseWorkspace> consumer) {
                Project project = executionInput.getProject();
                DatabaseLiquibaseManager liquibaseManager = DatabaseLiquibaseManager.getInstance(project);
                liquibaseManager.openWorkspaceCreationDialog(
                        connection.getDatabaseType(),
                        consumer);
            }
        });
        onSelectionChange(workspaceSelector, value -> {
            updateWorkspacePath();
            markFormChanged();
        });
    }

    private void updateWorkspacePath() {
        LiquibaseWorkspace workspace = workspaceSelector.getSelectedValue();
        if (workspace == null) {
            workspacePathLabel.setText("");
            return;
        }

        try {
            workspacePathLabel.setText(new LiquibaseWorkspacePaths(workspace).getLiquibaseRootPath().toString());
        } catch (IllegalArgumentException e) {
            workspacePathLabel.setText("");
        }
    }

    private void initSourceContextSelectors() {
        ConnectionHandler sourceConnection = executionInput.getSourceConnection();
        FieldState state = executionInput.getOperation().getSourceContextState();
        initConnectionSelector(
                sourceConnectionLabel,
                sourceConnectionSelector,
                sourceSchemaSelector,
                getConnections(),
                sourceConnection,
                state);
        initSchemaSelector(
                sourceSchemaLabel,
                sourceSchemaSelector,
                () -> getSourceConnection(),
                () -> executionInput.getSourceSchema(),
                state);
    }

    private void initTargetContextSelectors() {
        ConnectionHandler targetConnection = executionInput.getTargetConnection();
        FieldState state = executionInput.getOperation().getTargetContextState();
        initConnectionSelector(
                targetConnectionLabel,
                targetConnectionSelector,
                targetSchemaSelector,
                getConnections(),
                targetConnection,
                state);
        initSchemaSelector(
                targetSchemaLabel,
                targetSchemaSelector,
                () -> getTargetConnection(),
                () -> executionInput.getTargetSchema(),
                state);
    }

    private void initConnectionSelector(
            @NotNull JLabel label,
            @NotNull DBNComboBox<ConnectionHandler> selector,
            @NotNull DBObjectSelector<DBSchema> schemaSelector,
            @NotNull List<ConnectionHandler> values,
            @Nullable ConnectionHandler selectedValue,
            @NotNull FieldState state) {
        boolean visible = state.isVisible();
        boolean enabled = state.isEditable();
        label.setVisible(visible);
        selector.setVisible(visible);
        if (!visible) return;

        selector.setValues(values);
        selector.setSelectedValue(selectedValue);

        if (enabled) {
            onSelectionChange(selector, value -> {
                schemaSelector.reloadValues();
                markFormChanged();
            });
        }
        setFormFieldEnabled(selector, "CONDITIONAL_AVAILABILITY", enabled);
        //selector.setEnabled(enabled);
    }

    private void initSchemaSelector(
            @NotNull JLabel label,
            @NotNull DBObjectSelector<DBSchema> selector,
            @NotNull Supplier<ConnectionHandler> connectionSupplier,
            @NotNull Supplier<DBSchema> schemaSupplier,
            @NotNull FieldState state) {
        boolean visible = state.isVisible();
        boolean enabled = state.isEditable();
        label.setVisible(visible);
        selector.setVisible(visible);
        if (!visible) return;

        selector.initialize(this, SCHEMA);
        selector.withConnectionContext(connectionSupplier);
        selector.withValueLoader(() -> {
                    ConnectionHandler connection = connectionSupplier.get();
                    return connection == null ? emptyList() : connection.getObjectBundle().getSchemas();
                });
        selector.withValuePreselector(() -> {
                    DBSchema schema = schemaSupplier.get();
                    return schema == null ? null : schema.getName();
                });

        selector.triggerLoad();
        setFormFieldEnabled(selector, "CONDITIONAL_AVAILABILITY", enabled);
        //selector.setEnabled(enabled);
    }

    @NotNull
    private List<ConnectionHandler> getConnections() {
        return ConnectionManager.getInstance(executionInput.getProject()).getConnections();
    }

    @Nullable
    private ConnectionHandler getSourceConnection() {
        return getSelection(sourceConnectionSelector);
    }

    @Override
    protected void initValidation() {
        LiquibaseOperation operation = executionInput.getOperation();
        addSelectionValidation(workspaceSelector, txt("msg.liquibase.error.WorkspaceRequired"));
        addValidation(sourceSchemaSelector,
                selector -> !operation.requiresSourceSchema() || selector.getSelectedItem() != null,
                txt("msg.shared.error.SelectSchema"));
        addValidation(targetConnectionSelector,
                selector -> !operation.requiresTargetSchema() || selector.getSelectedItem() != null,
                txt("msg.shared.error.SelectTargetConnection"));
        addValidation(targetSchemaSelector,
                selector -> !operation.requiresTargetSchema() || selector.getSelectedItem() != null,
                txt("msg.shared.error.SelectTargetSchema"));
        addValidation(rollbackCountSpinner,
                spinner -> operation != ROLLBACK || getSelection(rollbackTypeSelector) != COUNT || (Integer) spinner.getValue() > 0,
                txt("msg.liquibase.error.RollbackCountRequired"));
        addValidation(rollbackTagTextField,
                textField -> operation != ROLLBACK || getSelection(rollbackTypeSelector) != TAG || !textField.getText().trim().isEmpty(),
                txt("msg.liquibase.error.RollbackTagRequired"));
        addTextValidation(rollbackDateField.getTextField(),
                text -> !isRollbackType(DATE) || !text.trim().isEmpty(),
                txt("msg.liquibase.error.RollbackDateRequired"));
        addTextValidation(rollbackDateField.getTextField(), textField -> validateRollbackDateFormat(textField.getText()));
    }

    @NotNull
    public LiquibaseExecutionInput getExecutionInput() {
        return executionInput;
    }

    @Override
    public void applyFormChanges() {
        DBSchema sourceSchema = getSourceSchema();
        DBSchema targetSchema = getTargetSchema();
        LiquibaseWorkspace workspace = getSelection(workspaceSelector);

        executionInput.setSourceSchema(sourceSchema);
        executionInput.setTargetSchema(targetSchema);
        executionInput.setWorkspace(workspace);
        executionInput.setRollbackType(getSelection(rollbackTypeSelector));
        executionInput.setRollbackCount((Integer) rollbackCountSpinner.getValue());
        executionInput.setRollbackTag(rollbackTagTextField.getText().trim());
        executionInput.setRollbackDate(getText(rollbackDateField.getTextField()));

        if (workspace == null) return;
        if (sourceSchema == null && targetSchema == null) return;

        executionInput.getWorkspaces().rememberWorkspace(
                executionInput.getRelevantConnection().getConnectionId(),
                executionInput.getRelevantSchema().getSchemaId(),
                workspace);
    }

    @Nullable
    public DBSchema getTargetSchema() {
        return getSelection(targetSchemaSelector);
    }

    @Nullable
    public DBSchema getSourceSchema() {
        return getSelection(sourceSchemaSelector);
    }

    @Nullable
    private String validateRollbackDateFormat(@NotNull String value) {
        if (!isRollbackType(DATE) || value.trim().isEmpty()) return null;
        String text = value.trim();

        Formatter formatter = Formatter.getInstance(executionInput.getProject());
        try {
            formatter.parseDateTime(text);
            return null;
        } catch (java.text.ParseException e) {
            return txt("msg.shared.error.InvalidDateFormat",
                    formatter.getDatetimeFormatPattern(),
                    formatter.formatDateTime(new Date()));
        }
    }

    @Nullable
    private ConnectionHandler getTargetConnection() {
        return getSelection(targetConnectionSelector);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return executionInput.getOperation() == ROLLBACK ? rollbackTypeSelector : workspaceSelector;
    }
}
