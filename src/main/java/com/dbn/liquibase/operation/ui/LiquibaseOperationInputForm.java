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

package com.dbn.liquibase.operation.ui;

import com.dbn.common.environment.EnvironmentTypeId;
import com.dbn.common.routine.Consumer;
import com.dbn.common.state.StateAttributes;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.ValueFactory;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.form.field.FieldState;
import com.dbn.common.ui.info.DBNInfoLabel;
import com.dbn.common.ui.link.HyperLinkForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.DatabaseType;
import com.dbn.data.editor.ui.ListPopupValuesProvider;
import com.dbn.data.editor.ui.TextFieldWithPopup;
import com.dbn.data.editor.ui.calendar.CalendarPopupType;
import com.dbn.liquibase.DatabaseLiquibaseManager;
import com.dbn.liquibase.operation.LiquibaseFeatureSupport;
import com.dbn.liquibase.operation.LiquibaseOperationInput;
import com.dbn.liquibase.operation.LiquibaseRollbackInstruction;
import com.dbn.liquibase.operation.LiquibaseRollbackType;
import com.dbn.liquibase.operation.LiquibaseUpdateInstruction;
import com.dbn.liquibase.operation.LiquibaseUpdateType;
import com.dbn.liquibase.workspace.LiquibaseEnvironmentProfile;
import com.dbn.liquibase.workspace.LiquibaseWorkspace;
import com.dbn.liquibase.workspace.LiquibaseWorkspaceBundle;
import com.dbn.liquibase.workspace.LiquibaseWorkspacePaths;
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
import java.awt.BorderLayout;
import java.util.Date;
import java.util.List;
import java.util.Objects;
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
import static com.dbn.common.ui.util.Tooltips.setToolTipText;
import static com.dbn.common.util.Lists.filter;
import static com.dbn.liquibase.operation.LiquibaseFeature.CHANGELOG_AUTHOR;
import static com.dbn.liquibase.operation.LiquibaseFeature.CHANGELOG_TAG;
import static com.dbn.liquibase.operation.LiquibaseFeature.CHECKPOINT_TAG;
import static com.dbn.liquibase.operation.LiquibaseFeature.DATABASE_TAG;
import static com.dbn.liquibase.operation.LiquibaseFeature.DISTINCT_SCHEMAS;
import static com.dbn.liquibase.operation.LiquibaseFeature.ROLLBACK;
import static com.dbn.liquibase.operation.LiquibaseFeature.SOURCE_SCHEMA;
import static com.dbn.liquibase.operation.LiquibaseFeature.TARGET_SCHEMA;
import static com.dbn.liquibase.operation.LiquibaseFeature.UPDATE_INSTRUCTION;
import static com.dbn.liquibase.operation.LiquibaseFeature.WORKSPACE;
import static com.dbn.liquibase.operation.LiquibaseFeature.WORKSPACE_CREATION;
import static com.dbn.liquibase.operation.LiquibaseRollbackType.COUNT;
import static com.dbn.liquibase.operation.LiquibaseRollbackType.DATE;
import static com.dbn.liquibase.operation.LiquibaseRollbackType.TAG;
import static com.dbn.liquibase.operation.LiquibaseUpdateType.ALL;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.type.DBObjectType.SCHEMA;
import static java.util.Collections.emptyList;

public class LiquibaseOperationInputForm extends DBNFormBase {
    private static final @NonNls String ATTR_UPDATE_TYPE = "update-type";
    private static final @NonNls String ATTR_ROLLBACK_TYPE = "rollback-type";
    private static final @NonNls String ATTR_CHANGELOG_AUTHOR = "changelog-author";

    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel rollbackDateFieldPanel;
    private JPanel rollbackTagFieldPanel;
    private JPanel hyperlinkPanel;
    private JLabel workspaceLabel;
    private JLabel environmentProfileLabel;
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
    private JLabel updateTypeLabel;
    private JLabel updateCountLabel;
    private JLabel updateTagLabel;
    private JSpinner rollbackCountSpinner;
    private JTextField changelogAuthorTextField;
    private JTextField databaseTagTextField;
    private JTextField checkpointTagTextField;
    private JTextField updateTagTextField;
    private DBNComboBox<ConnectionHandler> sourceConnectionSelector;
    private DBNComboBox<ConnectionHandler> targetConnectionSelector;
    private DBNComboBox<LiquibaseWorkspace> workspaceSelector;
    private DBNComboBox<LiquibaseEnvironmentProfile> envProfileSelector;
    private DBNComboBox<LiquibaseRollbackType> rollbackTypeSelector;
    private DBNInfoLabel rollbackCountInfoLabel;
    private DBNInfoLabel rollbackTagInfoLabel;
    private DBNInfoLabel rollbackDateInfoLabel;
    private DBNInfoLabel databaseTagInfoLabel;
    private DBNInfoLabel checkpointTagInfoLabel;
    private DBNInfoLabel updateCountInfoLabel;
    private DBNInfoLabel updateTagInfoLabel;
    private JSpinner updateCountSpinner;
    private DBNComboBox<LiquibaseUpdateType> updateTypeSelector;
    private TextFieldWithPopup<?> rollbackDateField;
    private DBObjectSelector<DBSchema> sourceSchemaSelector;
    private DBObjectSelector<DBSchema> targetSchemaSelector;
    private TextFieldWithPopup<?> rollbackTagField;

    private final LiquibaseOperationInput executionInput;
    private volatile ConnectionHandler environmentProfileConnection;

    LiquibaseOperationInputForm(@NotNull LiquibaseOperationInputDialog parent) {
        this(parent, parent.getExecutionInput());
    }

    public LiquibaseOperationInputForm(
            @NotNull DBNDialog<?> parent,
            @NotNull LiquibaseOperationInput executionInput) {
        super(parent);
        this.executionInput = executionInput;

        initHeaderPanel();
        initHintPanel();
        initHyperlinkPanel();
        initInfoLabels();
        initContextLabels();
        initWorkspaceSelector();
        initOperationTagField();
        initUpdateFields();
        initRollbackFields();
        initSourceContextSelectors();
        initTargetContextSelectors();
        initEnvironmentProfileSelector();
        executionInput.setWorkspace(workspaceSelector.getSelectedValue());
    }

    private void initHyperlinkPanel() {
        String documentationUrl = executionInput.getDocumentationUrl();
        if (documentationUrl.isEmpty()) return;

        HyperLinkForm hyperlinkForm = HyperLinkForm.create(
                "",
                txt("app.liquibase.link.LiquibaseDocumentation"),
                documentationUrl);

        hyperlinkPanel.add(hyperlinkForm.getComponent(), BorderLayout.EAST);
    }

    private void initInfoLabels() {
        rollbackCountInfoLabel.setContent(plain(txt("app.liquibase.hint.RollbackCount")));
        rollbackTagInfoLabel.setContent(plain(txt("app.liquibase.hint.RollbackTag")));
        rollbackDateInfoLabel.setContent(plain(txt("app.liquibase.hint.RollbackDate")));
        databaseTagInfoLabel.setContent(plain(txt("app.liquibase.hint.DatabaseTag")));
        checkpointTagInfoLabel.setContent(plain(txt("app.liquibase.hint.CheckpointTag")));
        updateCountInfoLabel.setContent(plain(txt("app.liquibase.hint.UpdateCount")));
        updateTagInfoLabel.setContent(plain(txt("app.liquibase.hint.UpdateTag")));
    }

    private void initOperationTagField() {
        LiquibaseFeatureSupport support = executionInput.getSupport();
        initChangelogAuthorField(support);
        initDatabaseTagField(support);
        initCheckpointTagField(support);
    }

    private void initChangelogAuthorField(LiquibaseFeatureSupport support) {
        boolean supported = support.supports(CHANGELOG_AUTHOR);
        changelogAuthorLabel.setVisible(supported);
        changelogAuthorTextField.setVisible(supported);
        if (supported) {
            DatabaseLiquibaseManager liquibaseManager = getLiquibaseManager();
            StateAttributes state = liquibaseManager.getState("EXECUTION_INPUT");
            initPersistence(changelogAuthorTextField, state, ATTR_CHANGELOG_AUTHOR, SystemProperties.getUserName());
        }
    }

    private @NotNull DatabaseLiquibaseManager getLiquibaseManager() {
        return DatabaseLiquibaseManager.getInstance(executionInput.getProject());
    }

    private void initDatabaseTagField(LiquibaseFeatureSupport support) {
        boolean databaseTag = support.supports(DATABASE_TAG);
        boolean changelogTag = support.supports(CHANGELOG_TAG);
        boolean supported = databaseTag || changelogTag;
        databaseTagLabel.setVisible(supported);
        databaseTagTextField.setVisible(supported);
        databaseTagInfoLabel.setVisible(supported);
        if (!supported) return;

        if (changelogTag) {
            databaseTagLabel.setText(txt("app.liquibase.label.ChangelogTag"));
            databaseTagInfoLabel.setContent(plain(txt("app.liquibase.hint.ChangelogTag")));
            setText(databaseTagTextField, executionInput.getChangelogTag());
        } else {
            setText(databaseTagTextField, executionInput.getDatabaseTag());
        }
    }

    private void initCheckpointTagField(LiquibaseFeatureSupport support) {
        boolean supported = support.supports(CHECKPOINT_TAG);
        checkpointTagLabel.setVisible(supported);
        checkpointTagTextField.setVisible(supported);
        checkpointTagInfoLabel.setVisible(supported);
        if (supported) setText(checkpointTagTextField, executionInput.getCheckpointTag());
    }

    private void initUpdateFields() {
        LiquibaseFeatureSupport support = executionInput.getSupport();
        if (!support.supports(UPDATE_INSTRUCTION)) return;

        StateAttributes state = getLiquibaseManager().getState("EXECUTION_INPUT");
        updateTypeSelector.setValues(List.of(LiquibaseUpdateType.values()));
        initPersistence(updateTypeSelector, state, ATTR_UPDATE_TYPE, ALL.id());

        LiquibaseUpdateInstruction updateInstruction = executionInput.getUpdateInstruction();
        updateInstruction.setType(getSelection(updateTypeSelector));
        updateCountSpinner.setModel(new SpinnerNumberModel(updateInstruction.getCount(), 1, Integer.MAX_VALUE, 1));
        setText(updateTagTextField, updateInstruction.getTag());
        onSelectionChange(updateTypeSelector, value -> {
            updateFieldAvailability();
            markFormChanged();
        });
    }

    private void initRollbackFields() {
        LiquibaseFeatureSupport support = executionInput.getSupport();
        if (!support.supports(ROLLBACK)) return;

        Project project = executionInput.getProject();
        rollbackTagField = new TextFieldWithPopup<>(project);
        rollbackTagFieldPanel.add(rollbackTagField);

        rollbackDateField = new TextFieldWithPopup<>(project);
        rollbackDateFieldPanel.add(rollbackDateField);

        rollbackTagField.createValuesListPopup(new ListPopupValuesProvider() {
            @Override
            public String getName() {
                return txt("app.liquibase.label.RollbackTag");
            }

            @Override
            public List<String> getValues() {
                return getRollbackTagValues();
            }
        }, null, true);
        rollbackDateField.createCalendarPopup(false, CalendarPopupType.DATE);

        DatabaseLiquibaseManager liquibaseManager = getLiquibaseManager();
        StateAttributes state = liquibaseManager.getState("EXECUTION_INPUT");
        rollbackTypeSelector.setValues(List.of(LiquibaseRollbackType.values()));
        initPersistence(rollbackTypeSelector, state, ATTR_ROLLBACK_TYPE, COUNT.id());

        LiquibaseRollbackInstruction rollbackInstruction = executionInput.getRollbackInstruction();
        rollbackInstruction.setType(rollbackTypeSelector.getSelectedValue());
        rollbackCountLabel.setVisible(true);
        rollbackCountSpinner.setVisible(true);
        rollbackCountSpinner.setModel(new SpinnerNumberModel(rollbackInstruction.getCount(), 1, Integer.MAX_VALUE, 1));
        setText(rollbackTagField.getTextField(), rollbackInstruction.getTag());
        setText(rollbackDateField.getTextField(), formatRollbackDate(rollbackInstruction.getDate()));
        onSelectionChange(rollbackTypeSelector, value -> {
            updateFieldAvailability();
            markFormChanged();
        });
    }

    private List<String> getRollbackTagValues() {
        DBSchema schema = executionInput.getTargetSchema();
        if (schema == null) return emptyList();

        DatabaseLiquibaseManager liquibaseManager = getLiquibaseManager();
                return liquibaseManager.getTags(
                schema.getConnectionId(),
                schema.getSchemaId());
    }

    @Override
    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsVisibility(() -> isUpdateOperation(), array(
                updateTypeLabel,
                updateTypeSelector));
        fieldAdapter.initFieldsVisibility(() -> isUpdateType(LiquibaseUpdateType.COUNT), array(
                updateCountLabel,
                updateCountSpinner,
                updateCountInfoLabel));
        fieldAdapter.initFieldsVisibility(() -> isUpdateType(LiquibaseUpdateType.TAG), array(
                updateTagLabel,
                updateTagTextField,
                updateTagInfoLabel));
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
        return executionInput.getSupport().supports(ROLLBACK);
    }

    private boolean isUpdateOperation() {
        return executionInput.getSupport().supports(UPDATE_INSTRUCTION);
    }

    private boolean isUpdateType(@NotNull LiquibaseUpdateType type) {
        return isUpdateOperation() && getSelection(updateTypeSelector) == type;
    }

    private boolean isRollbackType(@NotNull LiquibaseRollbackType type) {
        return isRollbackOperation() && getSelection(rollbackTypeSelector) == type;
    }

    private void initHeaderPanel() {
        ConnectionHandler connection = executionInput.getRelevantConnection();
        headerPanel.add(new DBNHeaderForm(this, connection).getComponent());
    }

    private void initHintPanel() {
        TextContent hint = plain(executionInput.getHint());
        hintPanel.add(new DBNHintForm(this, hint, null, true).getComponent());
    }

    private void initContextLabels() {
        LiquibaseFeatureSupport support = executionInput.getSupport();
        boolean sourceVisible = support.getSourceContextState().isVisible();
        boolean targetVisible = support.getTargetContextState().isVisible();
        boolean qualified = sourceVisible && targetVisible;

        sourceConnectionLabel.setText(txt(qualified ?
                "app.liquibase.label.SourceConnection" :
                "app.object.label.Connection"));
        sourceSchemaLabel.setText(txt(qualified ?
                "app.liquibase.label.SourceSchema" :
                "app.object.label.Schema"));
        targetConnectionLabel.setText(txt(qualified ?
                "app.liquibase.label.TargetConnection" :
                "app.object.label.Connection"));
        targetSchemaLabel.setText(txt(qualified ?
                "app.liquibase.label.TargetSchema" :
                "app.object.label.Schema"));
    }

    private void initWorkspaceSelector() {
        LiquibaseFeatureSupport support = executionInput.getSupport();
        boolean visible = support.requires(WORKSPACE);
        workspaceLabel.setVisible(visible);
        workspaceSelector.setVisible(visible);
        if (!visible) return;

        LiquibaseWorkspaceBundle workspaces = executionInput.getWorkspaces();
        ConnectionHandler connection = executionInput.getRelevantConnection();
        DBSchema schema = executionInput.getRelevantSchema();

        DatabaseType databaseType = connection.getDatabaseType();
        List<LiquibaseWorkspace> availableWorkspaces = workspaces.getWorkspaces(databaseType);

        LiquibaseWorkspace selectedWorkspace = workspaces.getSelectedWorkspace(
                schema.getConnectionId(),
                schema.getSchemaId());
        workspaceSelector.setValues(availableWorkspaces);
        workspaceSelector.setSelectedValue(availableWorkspaces.contains(selectedWorkspace) ? selectedWorkspace : null);
        if (support.supports(WORKSPACE_CREATION)) {
            workspaceSelector.withValueFactory(workspaceFactory(connection));
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

    private @NotNull ValueFactory<LiquibaseWorkspace> workspaceFactory(ConnectionHandler connection) {
        return new ValueFactory<>(txt("app.liquibase.action.NewWorkspace")) {
            @Override
            public void createValue(Consumer<LiquibaseWorkspace> consumer) {
                DatabaseLiquibaseManager liquibaseManager = getLiquibaseManager();
                liquibaseManager.openWorkspaceCreationDialog(
                        connection.getDatabaseType(),
                        consumer);
            }
        };
    }

    private void updateWorkspacePath() {
        LiquibaseWorkspace workspace = workspaceSelector.getSelectedValue();
        if (workspace == null) {
            setToolTipText(workspaceSelector, null);
            return;
        }

        try {
            setToolTipText(workspaceSelector, new LiquibaseWorkspacePaths(workspace).getLiquibaseRootPath().toString());
        } catch (IllegalArgumentException e) {
            setToolTipText(workspaceSelector, null);
        }
    }

    private void initEnvironmentProfileSelector() {
        setEmptyOptionsText(envProfileSelector, txt("app.liquibase.placeholder.NoEnvironmentProfile"));
        envProfileSelector.withValueLoader(() -> loadEnvironmentProfiles());
        envProfileSelector.withValueFactory(environmentProfileFactory());
        updateEnvironmentProfileSelector();
        onSelectionChange(envProfileSelector, value -> {
            executionInput.setEnvironmentProfile(value);
            markFormChanged();
        });
    }

    private @NotNull ValueFactory<LiquibaseEnvironmentProfile> environmentProfileFactory() {
        return new ValueFactory<>(txt("app.liquibase.placeholder.NewEnvironmentProfile")) {
            @Override
            public void createValue(Consumer<LiquibaseEnvironmentProfile> consumer) {
                ConnectionHandler connection = getRelevantContextConnection();
                if (connection == null) return;

                EnvironmentTypeId environmentTypeId = connection.getEnvironmentType().getId();
                DatabaseLiquibaseManager liquibaseManager = getLiquibaseManager();
                liquibaseManager.openEnvironmentProfileCreationDialog(environmentTypeId, consumer);
            }
        };
    }

    private void updateEnvironmentProfileSelector() {
        environmentProfileConnection = getRelevantContextConnection();
        setFormFieldEnabled(envProfileSelector, "CONDITIONAL_AVAILABILITY", environmentProfileConnection != null);
        envProfileSelector.reloadValues();
    }

    @NotNull
    private List<LiquibaseEnvironmentProfile> loadEnvironmentProfiles() {
        ConnectionHandler connection = environmentProfileConnection;
        if (connection == null) return emptyList();

        EnvironmentTypeId environmentTypeId = connection.getEnvironmentType().getId();
        return executionInput.getEnvironmentProfiles().getProfiles(environmentTypeId);
    }

    @Nullable
    private ConnectionHandler getRelevantContextConnection() {
        ConnectionHandler source = getSourceConnection();
        ConnectionHandler target = getTargetConnection();
        if (executionInput.getOperation().requires(SOURCE_SCHEMA)) return source == null ? target : source;
        return target == null ? source : target;
    }

    @NotNull
    private String getNoWorkspacesMessage() {
        return txt(
                "msg.liquibase.message.NoWorkspacesAvailable",
                executionInput.getRelevantConnection().getDatabaseType().getName());
    }

    private void initSourceContextSelectors() {
        FieldState state = executionInput.getSupport().getSourceContextState();
        initConnectionSelector(
                sourceConnectionLabel,
                sourceConnectionSelector,
                sourceSchemaSelector,
                getConnections(),
                executionInput.getSourceConnection(),
                state);
        initSchemaSelector(
                sourceSchemaLabel,
                sourceSchemaSelector,
                () -> getSourceConnection(),
                () -> executionInput.getSourceSchema(),
                state,
                null);
    }

    private void initTargetContextSelectors() {
        FieldState state = executionInput.getSupport().getTargetContextState();
        initConnectionSelector(
                targetConnectionLabel,
                targetConnectionSelector,
                targetSchemaSelector,
                getConnections(),
                executionInput.getTargetConnection(),
                state);
        initSchemaSelector(
                targetSchemaLabel,
                targetSchemaSelector,
                () -> getTargetConnection(),
                () -> executionInput.getTargetSchema(),
                state,
                () -> executionInput.getExcludedTargetSchema());
    }

    private void updateTargetConnections() {
        LiquibaseFeatureSupport support = executionInput.getSupport();
        if (!support.supports(DISTINCT_SCHEMAS)) return;

        ConnectionHandler targetConnection = getTargetConnection();
        FieldState state = support.getTargetContextState();
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

        LiquibaseWorkspaceBundle workspaces = executionInput.getWorkspaces();
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
                updateEnvironmentProfileSelector();
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
            @NotNull FieldState state,
            @Nullable Supplier<DBSchema> excludedSchemaSupplier) {
        boolean visible = state.isVisible();
        boolean enabled = state.isEditable();
        label.setVisible(visible);
        selector.setVisible(visible);
        if (!visible) return;

        selector.initialize(this, SCHEMA);
        selector.withConnectionContext(connectionSupplier);
        selector.withValueLoader(() -> {
                    ConnectionHandler connection = connectionSupplier.get();
                    DBSchema excludedSchema = excludedSchemaSupplier == null ? null : excludedSchemaSupplier.get();
                    return connection == null ? emptyList() : filter(connection.getObjectBundle().getSchemas(), s ->
                            !s.isSystemSchema() && !isSameSchema(s, excludedSchema));
                });
        selector.withValuePreselector(() -> {
                    DBSchema schema = schemaSupplier.get();
                    return schema == null ? null : schema.getName();
                });

        selector.triggerLoad();
        setFormFieldEnabled(selector, "CONDITIONAL_AVAILABILITY", enabled);
        //selector.setEnabled(enabled);
    }

    private static boolean isSameSchema(@NotNull DBSchema schema, @Nullable DBSchema other) {
        return other != null &&
                Objects.equals(schema.getConnectionId(), other.getConnectionId()) &&
                Objects.equals(schema.getSchemaId(), other.getSchemaId());
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
        LiquibaseFeatureSupport support = executionInput.getSupport();

        if (support.requires(WORKSPACE)) {
            addSelectionValidation(workspaceSelector, txt("msg.liquibase.error.WorkspaceRequired"));
        }
        addValidation(sourceSchemaSelector,
                selector -> !support.requires(SOURCE_SCHEMA) || selector.getSelectedItem() != null,
                txt("msg.shared.error.SelectSchema"));
        addValidation(targetConnectionSelector,
                selector -> !support.requires(TARGET_SCHEMA) || selector.getSelectedItem() != null,
                txt("msg.shared.error.SelectTargetConnection"));
        addValidation(targetSchemaSelector,
                selector -> !support.requires(TARGET_SCHEMA) || selector.getSelectedItem() != null,
                txt("msg.shared.error.SelectTargetSchema"));

        if (support.supports(UPDATE_INSTRUCTION)) {
            addValidation(updateCountSpinner,
                    spinner -> !isUpdateType(LiquibaseUpdateType.COUNT) || (Integer) spinner.getValue() > 0,
                    txt("msg.liquibase.error.UpdateCountRequired"));
            addTextValidation(updateTagTextField,
                    text -> !isUpdateType(LiquibaseUpdateType.TAG) || !text.trim().isEmpty(),
                    txt("msg.liquibase.error.UpdateTagRequired"));
        }

        if (support.supports(ROLLBACK)) {
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
        if (support.supports(UPDATE_INSTRUCTION)) {
            LiquibaseUpdateInstruction updateInstruction = executionInput.getUpdateInstruction();
            updateInstruction.setType(getSelection(updateTypeSelector));
            updateInstruction.setCount((Integer) updateCountSpinner.getValue());
            updateInstruction.setTag(getText(updateTagTextField));
        }

        addTextValidation(databaseTagTextField,
                text -> (!support.requires(DATABASE_TAG) && !support.requires(CHANGELOG_TAG)) || !text.trim().isEmpty(),
                txt(support.requires(CHANGELOG_TAG) ? "msg.liquibase.error.ChangelogTagRequired" : "msg.liquibase.error.DatabaseTagRequired"));
    }

    @NotNull
    public LiquibaseOperationInput getExecutionInput() {
        return executionInput;
    }

    @Override
    public void applyFormChanges() {
        LiquibaseFeatureSupport support = executionInput.getSupport();
        DBSchema sourceSchema = getSourceSchema();
        DBSchema targetSchema = getTargetSchema();
        LiquibaseWorkspace workspace = getSelection(workspaceSelector);
        LiquibaseEnvironmentProfile environmentProfile = getSelection(envProfileSelector);

        executionInput.setSourceSchema(sourceSchema);
        executionInput.setTargetSchema(targetSchema);
        executionInput.setWorkspace(workspace);
        executionInput.setEnvironmentProfile(environmentProfile);

        if (support.supports(ROLLBACK)) {
            LiquibaseRollbackInstruction rollbackInstruction = executionInput.getRollbackInstruction();
            rollbackInstruction.setType(getSelection(rollbackTypeSelector));
            rollbackInstruction.setCount((Integer) rollbackCountSpinner.getValue());
            rollbackInstruction.setTag(getText(rollbackTagField.getTextField()));
            rollbackInstruction.setDate(parseRollbackDate(getText(rollbackDateField.getTextField())));
        }
        executionInput.setChangelogAuthor(getText(changelogAuthorTextField));
        executionInput.setDatabaseTag(getText(databaseTagTextField));
        executionInput.setChangelogTag(support.supports(CHANGELOG_TAG) ? getText(databaseTagTextField) : null);
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
        LiquibaseFeatureSupport support = executionInput.getSupport();
        if (support.getTargetContextState().isVisible()) {
            return getSelection(targetSchemaSelector);
        }
        return executionInput.getTargetSchema();
    }

    @Nullable
    public DBSchema getSourceSchema() {
        LiquibaseFeatureSupport support = executionInput.getSupport();
        if (support.getSourceContextState().isVisible()) {
            return getSelection(sourceSchemaSelector);
        }
        return executionInput.getSourceSchema();
    }

    @Nullable
    private String validateRollbackDateFormat(@NotNull String value) {
        if (!isRollbackType(DATE) || value.trim().isEmpty()) return null;
        String text = value.trim();

        try {
            ensureFormatter().parseDateTime(text);
            return null;
        } catch (java.text.ParseException e) {
            return txt("msg.shared.error.InvalidDateFormat",
                    ensureFormatter().getDatetimeFormatPattern(),
                    ensureFormatter().formatDateTime(new Date()));
        }
    }

    @Nullable
    private Date parseRollbackDate(@NotNull String value) {
        if (value.trim().isEmpty()) return null;
        try {
            return ensureFormatter().parseDateTime(value);
        } catch (java.text.ParseException e) {
            return null;
        }
    }

    @Nullable
    private String formatRollbackDate(@Nullable Date value) {
        return value == null ? null : ensureFormatter().formatDateTime(value);
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
