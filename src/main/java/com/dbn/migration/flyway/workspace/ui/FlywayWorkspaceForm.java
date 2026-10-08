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

package com.dbn.migration.flyway.workspace.ui;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.info.DBNCommentLabel;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.ui.link.Hyperlinks;
import com.dbn.common.ui.misc.ContentRootSelector;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.connection.DatabaseType;
import com.dbn.migration.flyway.workspace.FlywayWorkspace;
import com.dbn.migration.shared.workspace.DatabaseMigrationWorkspaceBundle;
import com.dbn.migration.shared.workspace.ui.DatabaseMigrationWorkspacesForm;
import com.intellij.openapi.Disposable;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.dbn.common.text.TextContent.plain;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.ui.util.Tooltips.setToolTipText;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.connection.DatabaseType.GENERIC;
import static com.dbn.connection.DatabaseType.MYSQL;
import static com.dbn.connection.DatabaseType.ORACLE;
import static com.dbn.connection.DatabaseType.POSTGRES;
import static com.dbn.migration.flyway.workspace.FlywayWorkspace.DEFAULT_CONFIGURATION_FILE;
import static com.dbn.migration.flyway.workspace.FlywayWorkspace.DEFAULT_MIGRATIONS_DIRECTORY;
import static com.dbn.migration.flyway.workspace.FlywayWorkspace.DEFAULT_ROOT_PATH;
import static com.dbn.migration.flyway.workspace.FlywayWorkspace.DEFAULT_USER_CONFIGURATION_FILE;
import static com.dbn.nls.NlsResources.txt;

/**
 * Details form for a named Flyway workspace.
 *
 * <p>The form edits the Flyway-specific paths while using the shared workspace
 * bundle for common project-root and duplicate validation.</p>
 */
public class FlywayWorkspaceForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel hintPanel;
    private DBNHyperlinkLabel documentationLink;
    private ContentRootSelector contentRootComboBox;
    private JBTextField nameTextField;
    private DBNComboBox<DatabaseType> databaseTypeSelector;
    private JBTextField rootPathTextField;
    private JBTextField migrationsDirectoryTextField;
    private JBTextField configurationFileTextField;
    private JBTextField userConfigurationFileTextField;
    private DBNCommentLabel rootPathInfoLabel;

    private final DatabaseMigrationWorkspaceBundle<FlywayWorkspace> workspaces;
    private final FlywayWorkspace workspace;
    private final DatabaseType databaseType;

    FlywayWorkspaceForm(@NotNull FlywayWorkspaceDialog parent) {
        this(parent,
                parent.getWorkspaces(),
                parent.getWorkspace(),
                parent.getDatabaseType());
    }

    public FlywayWorkspaceForm(
            @NotNull DBNComponent parent,
            @NotNull DatabaseMigrationWorkspaceBundle<FlywayWorkspace> workspaces,
            @NotNull FlywayWorkspace workspace) {
        this(parent, workspaces, workspace, null);
    }

    public FlywayWorkspaceForm(
            @NotNull DBNComponent parent,
            @NotNull DatabaseMigrationWorkspaceBundle<FlywayWorkspace> workspaces,
            @NotNull FlywayWorkspace workspace,
            @Nullable DatabaseType databaseType) {
        super(parent);
        this.workspaces = workspaces;
        this.workspace = workspace;
        this.databaseType = databaseType;
        initHintPanel();
        initHyperlinksPanel();
        initFields();
    }

    private void initHintPanel() {
        TextContent hint = plain(txt("cfg.flyway.hint.WorkspaceSettings"));
        hintPanel.add(new DBNHintForm(this, hint, null).getComponent());
    }

    private void initHyperlinksPanel() {
        Hyperlinks.initHyperlink(
                documentationLink,
                txt("app.flyway.link.FlywayDocumentation"),
                "https://documentation.red-gate.com/flyway");
    }

    private void initFields() {
        initContentRoots();
        initDatabaseTypes();
        initPlaceholders();
        resetFormChanges();
        initPathListeners();
        updatePathTooltips();
    }

    private void initContentRoots() {
        contentRootComboBox.setContentRoots(workspaces.getContentRoots());
    }

    private void initDatabaseTypes() {
        databaseTypeSelector.setValues(getDatabaseTypeValues());
        setSelection(databaseTypeSelector, workspace.getDatabaseType());
    }

    @NotNull
    private List<DatabaseType> getDatabaseTypeValues() {
        List<DatabaseType> values = List.of(GENERIC, ORACLE, MYSQL, POSTGRES);
        if (databaseType == null) return values;

        DatabaseType sourceType = databaseType == DatabaseType.UNKNOWN ? GENERIC : databaseType;
        return sourceType == GENERIC ? List.of(GENERIC) : List.of(GENERIC, sourceType);
    }

    private void initPlaceholders() {
        rootPathTextField.getEmptyText().setText(DEFAULT_ROOT_PATH);
        migrationsDirectoryTextField.getEmptyText().setText(DEFAULT_MIGRATIONS_DIRECTORY);
        configurationFileTextField.getEmptyText().setText(DEFAULT_CONFIGURATION_FILE);
        userConfigurationFileTextField.getEmptyText().setText(DEFAULT_USER_CONFIGURATION_FILE);
    }

    private void initPathListeners() {
        onTextChange(nameTextField, e -> updateWorkspaceName());
        onSelectionChange(contentRootComboBox, root -> updatePathTooltips());
        onTextChange(rootPathTextField, e -> updatePathTooltips());
        onTextChange(migrationsDirectoryTextField, e -> updatePathTooltips());
        onTextChange(configurationFileTextField, e -> updatePathTooltips());
        onTextChange(userConfigurationFileTextField, e -> updatePathTooltips());
    }

    private void updateWorkspaceName() {
        String oldName = workspace.getName();
        String newName = getText(nameTextField);
        if (getGeneratedRootPath(oldName).equals(getText(rootPathTextField))) {
            setText(rootPathTextField, getGeneratedRootPath(newName));
        }
        workspace.setName(newName);

        Disposable parent = ensureParentComponent();
        if (parent instanceof DatabaseMigrationWorkspacesForm<?, ?> bundleForm) {
            bundleForm.refreshWorkspaceList();
        }
    }

    @NotNull
    private static String getGeneratedRootPath(@Nullable String workspaceName) {
        if (isEmpty(workspaceName)) return DEFAULT_ROOT_PATH;

        String pathName = workspaceName.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
        return DEFAULT_ROOT_PATH + "/" + (pathName.equals(".") || pathName.equals("..") ? "_" : pathName);
    }

    private void updatePathTooltips() {
        String contentRoot = contentRootComboBox.getSelectedPath();
        if (isEmpty(contentRoot)) {
            rootPathInfoLabel.setText("");
            setToolTipText(rootPathTextField, null);
            setToolTipText(migrationsDirectoryTextField, null);
            setToolTipText(configurationFileTextField, null);
            setToolTipText(userConfigurationFileTextField, null);
            return;
        }

        String flywayRoot = appendPath(contentRoot, getText(rootPathTextField));
        rootPathInfoLabel.setText(flywayRoot);
        setToolTipText(rootPathTextField, flywayRoot);
        setToolTipText(migrationsDirectoryTextField, appendPath(flywayRoot, getText(migrationsDirectoryTextField)));
        setToolTipText(configurationFileTextField, appendPath(flywayRoot, getText(configurationFileTextField)));
        setToolTipText(userConfigurationFileTextField, appendPath(flywayRoot, getText(userConfigurationFileTextField)));
    }

    @NotNull
    private static String appendPath(@Nullable String parent, @Nullable String child) {
        if (isEmpty(parent)) return child == null ? "" : child;
        if (isEmpty(child)) return parent;
        return parent + "/" + child;
    }

    @Override
    protected void initValidation() {
        addRequiredTextValidation(nameTextField, txt("msg.flyway.error.WorkspaceNameRequired"));
        addSelectionValidation(databaseTypeSelector, txt("msg.flyway.error.DatabaseTypeRequired"));
        addValidation(nameTextField, field -> validateWorkspaceName());
        addSelectionValidation(contentRootComboBox, txt("msg.flyway.error.ContentRootRequired"));
        addValidation(rootPathTextField, field -> validateWorkspaceRoot());

        addRequiredTextValidation(rootPathTextField, txt("msg.flyway.error.RootPathRequired"));
        addRequiredTextValidation(migrationsDirectoryTextField, txt("msg.flyway.error.MigrationsDirectoryRequired"));
        addRequiredTextValidation(configurationFileTextField, txt("msg.flyway.error.ConfigurationFileRequired"));
        addRequiredTextValidation(userConfigurationFileTextField, txt("msg.flyway.error.UserConfigurationFileRequired"));

        addTextValidation(rootPathTextField, value -> isValidRelativePath(value, true), txt("msg.flyway.error.InvalidDirectoryPath"));
        addTextValidation(migrationsDirectoryTextField, value -> isValidRelativePath(value, false), txt("msg.flyway.error.InvalidDirectoryPath"));
        addTextValidation(configurationFileTextField, value -> isValidFileName(value), txt("msg.flyway.error.InvalidFileName"));
        addTextValidation(userConfigurationFileTextField, value -> isValidFileName(value), txt("msg.flyway.error.InvalidFileName"));
    }

    private String validateWorkspaceRoot() {
        String selectedPath = contentRootComboBox.getSelectedPath();
        if (selectedPath == null) return null;

        FlywayWorkspace owner = workspaces.findRootOwner(
                selectedPath,
                getText(rootPathTextField),
                workspace);
        return owner == null ? null : txt("msg.flyway.error.ContentRootAlreadyMapped", getWorkspaceName(owner));
    }

    private String validateWorkspaceName() {
        FlywayWorkspace owner = workspaces.findNameOwner(getText(nameTextField), workspace);
        return owner == null ? null : txt("msg.flyway.error.WorkspaceNameAlreadyUsed");
    }

    private String getWorkspaceName(FlywayWorkspace workspace) {
        return isEmpty(workspace.getName()) ? txt("app.shared.placeholder.Unnamed") : workspace.getName();
    }

    private boolean isValidRelativePath(String value, boolean allowCurrentDirectory) {
        try {
            Path path = Paths.get(value);
            return !path.isAbsolute() && path.getNameCount() > 0 &&
                    (allowCurrentDirectory || !".".equals(path.toString())) &&
                    !"..".equals(path.toString());
        } catch (InvalidPathException e) {
            return false;
        }
    }

    private boolean isValidFileName(String value) {
        try {
            Path path = Paths.get(value);
            return !path.isAbsolute() && path.getNameCount() == 1 &&
                    !".".equals(path.toString()) && !"..".equals(path.toString());
        } catch (InvalidPathException e) {
            return false;
        }
    }

    @Override
    public void resetFormChanges() {
        setText(nameTextField, workspace.getName());
        setSelection(databaseTypeSelector, workspace.getDatabaseType());
        contentRootComboBox.setSelectedPath(workspace.getContentRootPath());
        setText(rootPathTextField, workspace.getRootPath());
        setText(migrationsDirectoryTextField, workspace.getMigrationsDirectory());
        setText(configurationFileTextField, workspace.getConfigurationFile());
        setText(userConfigurationFileTextField, workspace.getUserConfigurationFile());
        updatePathTooltips();
    }

    @Override
    public void applyFormChanges() {
        workspace.setName(getText(nameTextField));
        workspace.setDatabaseType(getSelection(databaseTypeSelector));
        workspace.setContentRootPath(contentRootComboBox.getSelectedPath());
        workspace.setRootPath(getText(rootPathTextField));
        workspace.setMigrationsDirectory(getText(migrationsDirectoryTextField));
        workspace.setConfigurationFile(getText(configurationFileTextField));
        workspace.setUserConfigurationFile(getText(userConfigurationFileTextField));
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return nameTextField;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
