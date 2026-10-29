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

package com.dbn.migration.flyway.operation.ui;

import com.dbn.common.color.Colors;
import com.dbn.common.routine.Consumer;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.ValueFactory;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.connection.ConnectionHandler;
import com.dbn.migration.flyway.DatabaseFlywayManager;
import com.dbn.migration.flyway.operation.FlywayFeatureSupport;
import com.dbn.migration.flyway.operation.FlywayOperationInput;
import com.dbn.migration.flyway.workspace.FlywayWorkspace;
import com.dbn.migration.flyway.workspace.FlywayWorkspaceBundle;
import com.dbn.object.DBSchema;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.util.List;

import static com.dbn.common.ui.form.DBNHintForm.installHintForm;
import static com.dbn.common.ui.form.field.DBNFormFieldDisabler.setFormFieldEnabled;
import static com.dbn.common.ui.link.Hyperlinks.initHyperlink;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setEmptyOptionsText;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.Tooltips.setToolTipText;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.migration.flyway.operation.FlywayFeature.WORKSPACE;
import static com.dbn.migration.flyway.operation.FlywayFeature.WORKSPACE_CREATION;
import static com.dbn.nls.NlsResources.txt;

/**
 * Input form for a Flyway operation.
 */
public class FlywayOperationInputForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel hintPanel;
    private DBNHyperlinkLabel documentationLink;
    private JLabel workspaceLabel;
    private DBNComboBox<FlywayWorkspace> workspaceSelector;
    private JBTextField workspacePathTextField;
    private JBTextField migrationsPathTextField;

    private final FlywayOperationInput executionInput;

    public FlywayOperationInputForm(
            @NotNull DBNComponent parent,
            @NotNull FlywayOperationInput executionInput) {
        super(parent);
        this.executionInput = executionInput;
        workspacePathTextField.setEditable(false);
        migrationsPathTextField.setEditable(false);
        workspacePathTextField.setForeground(Colors.getContextHelpForeground());
        migrationsPathTextField.setForeground(Colors.getContextHelpForeground());
        initHintPanel();
        initDocumentationLink();
        initWorkspaceSelector();
    }

    private void initHintPanel() {
        installHintForm(this, TextContent.plain(executionInput.getOperation().getHint()), hintPanel);
    }

    private void initDocumentationLink() {
        initHyperlink(
                documentationLink,
                txt("app.flyway.link.FlywayDocumentation"),
                "https://documentation.red-gate.com/flyway");
    }

    private void initWorkspaceSelector() {
        FlywayFeatureSupport support = executionInput.getSupport();
        boolean visible = support.getWorkspaceState().isVisible();
        boolean editable = support.getWorkspaceState().isEditable();
        workspaceLabel.setVisible(visible);
        workspaceSelector.setVisible(visible);
        workspacePathTextField.setVisible(visible);
        migrationsPathTextField.setVisible(visible);
        if (!visible) return;
        setFormFieldEnabled(workspaceSelector, "CONDITIONAL_AVAILABILITY", editable);

        DBSchema schema = executionInput.getRelevantSchema();
        FlywayWorkspaceBundle workspaces = executionInput.getWorkspaces();
        List<FlywayWorkspace> values = workspaces.getWorkspaces(schema.getConnection().getDatabaseType());
        workspaceSelector.setValues(values);

        FlywayWorkspace workspace = executionInput.getWorkspace();
        if (workspace == null) {
            workspace = workspaces.getSelectedWorkspace(
                    schema.getConnectionId(),
                    schema.getSchemaId());
        }
        setSelection(workspaceSelector, workspace);
        if (support.supports(WORKSPACE_CREATION)) {
            workspaceSelector.withValueFactory(workspaceFactory(schema.getConnection()));
        } else if (values.isEmpty()) {
            setEmptyOptionsText(workspaceSelector, txt("msg.flyway.error.WorkspaceRequired"));
        }
        updateWorkspaceInfo(getSelection(workspaceSelector));
        onSelectionChange(workspaceSelector, this::updateWorkspaceInfo);
    }

    @NotNull
    private ValueFactory<FlywayWorkspace> workspaceFactory(@NotNull ConnectionHandler connection) {
        return new ValueFactory<>(txt("app.flyway.action.NewWorkspace")) {
            @Override
            public void createValue(Consumer<FlywayWorkspace> consumer) {
                DatabaseFlywayManager manager = DatabaseFlywayManager.getInstance(executionInput.getProject());
                manager.openWorkspaceCreationDialog(connection.getDatabaseType(), consumer);
            }
        };
    }

    private void updateWorkspaceInfo(@Nullable FlywayWorkspace workspace) {
        if (workspace == null) {
            workspacePathTextField.setText("");
            migrationsPathTextField.setText("");
            setToolTipText(workspacePathTextField, null);
            setToolTipText(migrationsPathTextField, null);
            return;
        }

        String rootPath = appendPath(workspace.getContentRootPath(), workspace.getRootPath());
        String migrationsPath = appendPath(rootPath, workspace.getMigrationsDirectory());
        workspacePathTextField.setText(rootPath);
        migrationsPathTextField.setText(migrationsPath);
        setToolTipText(workspacePathTextField, rootPath);
        setToolTipText(migrationsPathTextField, migrationsPath);
        markFormChanged();
    }

    @NotNull
    private static String appendPath(@Nullable String parent, @Nullable String child) {
        if (isEmpty(parent)) return child == null ? "" : child;
        if (isEmpty(child)) return parent;
        return parent + "/" + child;
    }

    @Override
    protected void initValidation() {
        FlywayFeatureSupport support = executionInput.getSupport();
        if (support.requires(WORKSPACE)) {
            addSelectionValidation(workspaceSelector, txt("msg.flyway.error.WorkspaceRequired"));
        }
    }

    @Override
    public void applyFormChanges() {
        FlywayWorkspace workspace = getSelection(workspaceSelector);
        executionInput.setWorkspace(workspace);

        if (workspace != null) {
            DBSchema schema = executionInput.getRelevantSchema();
            executionInput.getWorkspaces().rememberWorkspace(
                    schema.getConnectionId(),
                    schema.getSchemaId(),
                    workspace);
        }
    }

    @NotNull
    public FlywayOperationInput getExecutionInput() {
        return executionInput;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return workspaceSelector;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
