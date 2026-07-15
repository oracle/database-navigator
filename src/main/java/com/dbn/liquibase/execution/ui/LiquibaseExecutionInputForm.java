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
import com.dbn.common.state.StateAttributes;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.ValueFactory;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.form.field.FieldState;
import com.dbn.common.ui.info.DBNCommentLabel;
import com.dbn.common.ui.info.DBNInfoLabel;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.DatabaseType;
import com.dbn.data.editor.ui.ListPopupValuesProvider;
import com.dbn.data.editor.ui.TextFieldWithPopup;
import com.dbn.data.editor.ui.calendar.CalendarPopupType;
import com.dbn.liquibase.DatabaseLiquibaseManager;
import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseOperation;
import com.dbn.liquibase.execution.LiquibaseOperationSupport;
import com.dbn.liquibase.execution.LiquibaseRollbackType;
import com.dbn.liquibase.model.LiquibaseWorkspace;
import com.dbn.liquibase.model.LiquibaseWorkspaceBundle;
import com.dbn.liquibase.model.LiquibaseWorkspacePaths;
import com.dbn.object.DBSchema;
import com.dbn.object.common.ui.DBObjectSelector;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang3.SystemProperties;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import static com.dbn.common.text.TextContent.plain;
import static com.dbn.common.ui.form.DBNFormState.initPersistence;
import static com.dbn.common.ui.form.field.DBNFormFieldDisabler.setFormFieldEnabled;
import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setEmptyOptionsText;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.util.Lists.filter;
import static com.dbn.liquibase.execution.LiquibaseOperation.COMPARE_SCHEMAS;
import static com.dbn.liquibase.execution.LiquibaseRollbackType.COUNT;
import static com.dbn.liquibase.execution.LiquibaseRollbackType.DATE;
import static com.dbn.liquibase.execution.LiquibaseRollbackType.TAG;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.type.DBObjectType.SCHEMA;
import static java.util.Collections.emptyList;

public class LiquibaseExecutionInputForm extends DBNFormBase {
    private static final @NonNls String ATTR_ROLLBACK_TYPE = "rollback-type";
    private static final @NonNls String ATTR_CHANGELOG_AUTHOR = "changelog-author";

    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel rollbackDateFieldPanel;
    private JPanel rollbackTagFieldPanel;
    private JLabel workspaceLabel;
    private JLabel sourceConnectionLabel;
    private JLabel sourceSchemaLabel;
    private JLabel targetConnectionLabel;
    private JLabel targetSchemaLabel;
    private JLabel rollbackTypeLabel;
    private JLabel rollbackTagLabel;
    private JLabel rollbackDateLabel;
    private JLabel rollbackCountLabel;
    private JLabel changelogAuthorLabel;
    private JLabel databaseTagLabel;
    private JLabel checkpointTagLabel;
    private JSpinner rollbackCountSpinner;
    private JTextField changelogAuthorTextField;
    private JTextField databaseTagTextField;
    private JTextField checkpointTagTextField;
    private DBNComboBox<ConnectionHandler> sourceConnectionSelector;
    private DBNComboBox<ConnectionHandler> targetConnectionSelector;
    private DBNComboBox<LiquibaseWorkspace> workspaceSelector;
    private DBNComboBox<LiquibaseRollbackType> rollbackTypeSelector;
    private DBNCommentLabel workspacePathLabel;
    private DBNInfoLabel rollbackCountInfoLabel;
    private DBNInfoLabel rollbackTagInfoLabel;
    private DBNInfoLabel rollbackDateInfoLabel;
    private DBNInfoLabel checkpointTagInfoLabel;
    private TextFieldWithPopup<?> rollbackDateField;
    private DBObjectSelector<DBSchema> sourceSchemaSelector;
    private DBObjectSelector<DBSchema> targetSchemaSelector;
    private TextFieldWithPopup<?> rollbackTagField;

    private final LiquibaseExecutionInputDialog parent;
    private final LiquibaseExecutionInput executionInput;

    LiquibaseExecutionInputForm(@NotNull LiquibaseExecutionInputDialog parent) {
        super(parent);
        this.parent = parent;
        this.executionInput = parent.getExecutionInput();

        initHeaderPanel();
        initHintPanel();
        initInfoLabels();
        initContextLabels();
        initWorkspaceSelector();
        initOperationTagField();
        initRollbackFields();
        initSourceContextSelectors();
        initTargetContextSelectors();
        executionInput.setWorkspace(workspaceSelector.getSelectedValue());
    }

    private void initInfoLabels() {
        rollbackCountInfoLabel.setContent(plain(txt("cfg.liquibase.hint.RollbackCount")));
        rollbackTagInfoLabel.setContent(plain(txt("cfg.liquibase.hint.RollbackTag")));
        rollbackDateInfoLabel.setContent(plain(txt("cfg.liquibase.hint.RollbackDate")));
        checkpointTagInfoLabel.setContent(plain(txt("cfg.liquibase.hint.CheckpointTag")));
    }

    private void initOperationTagField() {
        LiquibaseOperation operation = executionInput.getOperation();
        LiquibaseOperationSupport support = operation.getSupport();
        initChangelogAuthorField(support);
        initDatabaseTagField(support);
        initCheckpointTagField(support);
    }

    private void initChangelogAuthorField(LiquibaseOperationSupport support) {
        boolean supported = support.supportsChangelogAuthor();
        changelogAuthorLabel.setVisible(supported);
        changelogAuthorTextField.setVisible(supported);
        if (supported) {
            DatabaseLiquibaseManager liquibaseManager = DatabaseLiquibaseManager.getInstance(executionInput.getProject());
            StateAttributes state = liquibaseManager.getState("EXECUTION_INPUT");
            initPersistence(changelogAuthorTextField, state, ATTR_CHANGELOG_AUTHOR, SystemProperties.getUserName());
        }
    }

    private void initDatabaseTagField(LiquibaseOperationSupport support) {
        boolean supported = support.supportsDatabaseTag();
        databaseTagLabel.setVisible(supported);
        databaseTagTextField.setVisible(supported);
        if (supported) setText(databaseTagTextField, executionInput.getDatabaseTag());
    }

    private void initCheckpointTagField(LiquibaseOperationSupport support) {
        boolean supported = support.supportsCheckpointTag();
        checkpointTagLabel.setVisible(supported);
        checkpointTagTextField.setVisible(supported);
        checkpointTagInfoLabel.setVisible(supported);
        if (supported) setText(checkpointTagTextField, executionInput.getCheckpointTag());
    }

    private void initRollbackFields() {
        LiquibaseOperationSupport support = executionInput.getOperation().getSupport();
        if (!support.supportsRollback()) return;

        Project project = executionInput.getProject();
        rollbackTagField = new TextFieldWithPopup<>(project);
        rollbackTagFieldPanel.add(rollbackTagField);

        rollbackDateField = new TextFieldWithPopup<>(project);
        rollbackDateFieldPanel.add(rollbackDateField);

        rollbackTagField.createValuesListPopup(new ListPopupValuesProvider() {
            @Override
            public String getName() {
                return txt("cfg.liquibase.label.RollbackTag");
            }

            @Override
            public List<String> getValues() {
                return getRollbackTagValues();
            }
        }, null, true);
        rollbackDateField.createCalendarPopup(false, CalendarPopupType.DATE);

        DatabaseLiquibaseManager liquibaseManager = DatabaseLiquibaseManager.getInstance(project);
        StateAttributes state = liquibaseManager.getState("EXECUTION_INPUT");
        rollbackTypeSelector.setValues(List.of(LiquibaseRollbackType.values()));
        initPersistence(rollbackTypeSelector, state, ATTR_ROLLBACK_TYPE, COUNT.id());

        executionInput.setRollbackType(rollbackTypeSelector.getSelectedValue());
        rollbackCountLabel.setVisible(true);
        rollbackCountSpinner.setVisible(true);
        rollbackCountSpinner.setModel(new SpinnerNumberModel(executionInput.getRollbackCount(), 1, Integer.MAX_VALUE, 1));
        setText(rollbackTagField.getTextField(), executionInput.getRollbackTag());
        setText(rollbackDateField.getTextField(), executionInput.getRollbackDate());
        onSelectionChange(rollbackTypeSelector, value -> {
            updateFieldAvailability();
            markFormChanged();
        });
    }

    private List<String> getRollbackTagValues() {
        LiquibaseWorkspace workspace = workspaceSelector.getSelectedValue();
        DBSchema schema = executionInput.getTargetSchema();
        return workspace == null || schema == null ?
                emptyList() :
                parent.getWorkspaces().getCheckpointTags(
                        workspace,
                        schema.getConnectionId(),
                        schema.getSchemaId());
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
                rollbackTagFieldPanel,
                rollbackTagInfoLabel));

        fieldAdapter.initFieldsVisibility(() -> isRollbackType(DATE), array(
                rollbackDateLabel,
                rollbackDateFieldPanel,
                rollbackDateInfoLabel));
    }

    private boolean isRollbackOperation() {
        return executionInput.getOperation().getSupport().supportsRollback();
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
        boolean sourceVisible = operation.getSupport().getSourceContextState().isVisible();
        boolean targetVisible = operation.getSupport().getTargetContextState().isVisible();
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
        boolean visible = executionInput.getOperation().getSupport().requiresWorkspace();
        workspaceLabel.setVisible(visible);
        workspaceSelector.setVisible(visible);
        workspacePathLabel.setVisible(visible);
        if (!visible) return;

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
        LiquibaseOperationSupport support = executionInput.getOperation().getSupport();
        if (support.supportsWorkspaceCreation()) {
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
        } else if (availableWorkspaces.isEmpty()) {
            setEmptyOptionsText(workspaceSelector, getNoWorkspacesMessage());
        }
        updateWorkspacePath();
        onSelectionChange(workspaceSelector, value -> {
            updateWorkspacePath();
            updateTargetConnections();
            markFormChanged();
        });
    }

    private void updateWorkspacePath() {
        LiquibaseWorkspace workspace = workspaceSelector.getSelectedValue();
        if (workspace == null) {
            String message = workspaceSelector.getItemCount() == 0 &&
                    !executionInput.getOperation().getSupport().supportsWorkspaceCreation() ?
                    getNoWorkspacesMessage() : null;
            workspacePathLabel.setText(message == null ? "" : message);
            return;
        }

        try {
            workspacePathLabel.setText(new LiquibaseWorkspacePaths(workspace).getLiquibaseRootPath().toString());
        } catch (IllegalArgumentException e) {
            workspacePathLabel.setText("");
        }
    }

    @NotNull
    private String getNoWorkspacesMessage() {
        return txt(
                "msg.liquibase.message.NoWorkspacesAvailable",
                executionInput.getRelevantConnection().getDatabaseType().getName());
    }

    private void initSourceContextSelectors() {
        ConnectionHandler sourceConnection = executionInput.getSourceConnection();
        FieldState state = executionInput.getOperation().getSupport().getSourceContextState();
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
        FieldState state = executionInput.getOperation().getSupport().getTargetContextState();
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

    private void updateTargetConnections() {
        if (executionInput.getOperation() != COMPARE_SCHEMAS) return;

        ConnectionHandler targetConnection = getTargetConnection();
        FieldState state = executionInput.getOperation().getSupport().getTargetContextState();
        List<ConnectionHandler> connections = getSupportedConnections(getConnections(), state);
        targetConnectionSelector.setValues(connections);
        targetConnectionSelector.setSelectedValue(connections.contains(targetConnection) ? targetConnection : null);
        targetSchemaSelector.reloadValues();
    }

    @NotNull
    private List<ConnectionHandler> getSupportedConnections(
            @NotNull List<ConnectionHandler> connections,
            @NotNull FieldState state) {
        if (!state.isEditable()) return connections;
        LiquibaseWorkspace workspace = workspaceSelector.getSelectedValue();
        if (workspace == null) return connections;

        LiquibaseWorkspaceBundle workspaces = parent.getWorkspaces();
        return filter(connections, c -> workspaces.isCompatible(workspace, c.getDatabaseType()));
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

        List<ConnectionHandler> supportedValues = getSupportedConnections(values, state);
        selector.setValues(supportedValues);
        selector.setSelectedValue(supportedValues.contains(selectedValue) ? selectedValue : null);

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
                    return connection == null ? emptyList() : filter(connection.getObjectBundle().getSchemas(), s -> !s.isSystemSchema());
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
        LiquibaseOperationSupport support = operation.getSupport();

        if (support.requiresWorkspace()) {
            addSelectionValidation(workspaceSelector, txt("msg.liquibase.error.WorkspaceRequired"));
        }
        addValidation(sourceSchemaSelector,
                selector -> !support.requiresSourceSchema() || selector.getSelectedItem() != null,
                txt("msg.shared.error.SelectSchema"));
        addValidation(targetConnectionSelector,
                selector -> !support.requiresTargetSchema() || selector.getSelectedItem() != null,
                txt("msg.shared.error.SelectTargetConnection"));
        addValidation(targetSchemaSelector,
                selector -> !support.requiresTargetSchema() || selector.getSelectedItem() != null,
                txt("msg.shared.error.SelectTargetSchema"));

        if (support.supportsRollback()) {
            addValidation(rollbackCountSpinner,
                    spinner -> getSelection(rollbackTypeSelector) != COUNT || (Integer) spinner.getValue() > 0,
                    txt("msg.liquibase.error.RollbackCountRequired"));
            addTextValidation(rollbackTagField.getTextField(),
                    text -> getSelection(rollbackTypeSelector) != TAG || !text.trim().isEmpty(),
                    txt("msg.liquibase.error.RollbackTagRequired"));
            addTextValidation(rollbackDateField.getTextField(),
                    text -> !isRollbackType(DATE) || !text.trim().isEmpty(),
                    txt("msg.liquibase.error.RollbackDateRequired"));
            addTextValidation(rollbackDateField.getTextField(), textField -> validateRollbackDateFormat(textField.getText()));
        }

        addTextValidation(databaseTagTextField,
                text -> !support.requiresDatabaseTag() || !text.trim().isEmpty(),
                txt("msg.liquibase.error.DatabaseTagRequired"));
    }

    @NotNull
    public LiquibaseExecutionInput getExecutionInput() {
        return executionInput;
    }

    @Override
    public void applyFormChanges() {
        LiquibaseOperationSupport support = executionInput.getOperation().getSupport();
        DBSchema sourceSchema = getSourceSchema();
        DBSchema targetSchema = getTargetSchema();
        LiquibaseWorkspace workspace = getSelection(workspaceSelector);

        executionInput.setSourceSchema(sourceSchema);
        executionInput.setTargetSchema(targetSchema);
        executionInput.setWorkspace(workspace);

        if (support.supportsRollback()) {
            executionInput.setRollbackType(getSelection(rollbackTypeSelector));
            executionInput.setRollbackCount((Integer) rollbackCountSpinner.getValue());
            executionInput.setRollbackTag(getText(rollbackTagField.getTextField()));
            executionInput.setRollbackDate(getText(rollbackDateField.getTextField()));
        }
        executionInput.setChangelogAuthor(getText(changelogAuthorTextField));
        executionInput.setDatabaseTag(getText(databaseTagTextField));
        executionInput.setCheckpointTag(getText(checkpointTagTextField));

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
}
