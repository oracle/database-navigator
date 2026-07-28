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

package com.dbn.liquibase.workspace.ui;

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
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.dbn.liquibase.workspace.LiquibaseChangelogFiles;
import com.dbn.liquibase.workspace.LiquibaseChangelogFormat;
import com.dbn.liquibase.workspace.LiquibaseWorkspace;
import com.dbn.liquibase.workspace.LiquibaseWorkspaceBundle;
import com.dbn.liquibase.workspace.LiquibaseWorkspacePaths;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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
import static com.dbn.liquibase.workspace.LiquibaseChangelogFiles.getDefaultMasterChangelog;
import static com.dbn.liquibase.workspace.LiquibaseWorkspace.DEFAULT_CHANGELOG_DIRECTORY;
import static com.dbn.liquibase.workspace.LiquibaseWorkspace.DEFAULT_DOCUMENTATION_DIRECTORY;
import static com.dbn.liquibase.workspace.LiquibaseWorkspace.DEFAULT_PROPERTIES_FILE;
import static com.dbn.liquibase.workspace.LiquibaseWorkspace.DEFAULT_ROOT_PATH;
import static com.dbn.liquibase.workspace.LiquibaseWorkspace.DEFAULT_SQL_DIRECTORY;
import static com.dbn.nls.NlsResources.txt;

public class LiquibaseWorkspaceForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel hintPanel;
    private DBNHyperlinkLabel documentationLink;
    private ContentRootSelector contentRootComboBox;
    private JBTextField nameTextField;
    private DBNComboBox<DatabaseType> databaseTypeSelector;
    private DBNComboBox<LiquibaseChangelogFormat> changelogFormatSelector;
    private JBTextField rootPathTextField;
    private JBTextField changelogDirectoryTextField;
    private JBTextField sqlDirectoryTextField;
    private JBTextField documentationDirectoryTextField;
    private JBTextField masterChangelogTextField;
    private JBTextField propertiesFileTextField;
    private DBNCommentLabel rootPathInfoLabel;

    private final LiquibaseWorkspaceBundle workspaces;
    private final LiquibaseWorkspace workspace;
    private final DatabaseType databaseType;

    LiquibaseWorkspaceForm(@NotNull LiquibaseWorkspaceDialog parent) {
        this(parent,
                parent.getWorkspaces(),
                parent.getWorkspace(),
                parent.getDatabaseType());
    }

    LiquibaseWorkspaceForm(
            @NotNull DBNComponent parent,
            @NotNull LiquibaseWorkspaceBundle workspaces,
            @NotNull LiquibaseWorkspace workspace) {
        this(parent, workspaces, workspace, null);
    }

    LiquibaseWorkspaceForm(
            @NotNull DBNComponent parent,
            @NotNull LiquibaseWorkspaceBundle workspaces,
            @NotNull LiquibaseWorkspace workspace,
            @Nullable DatabaseType databaseType) {
        super(parent);
        this.workspaces = workspaces;
        this.workspace = workspace;
        this.databaseType = databaseType;
        initHeaderPanel();
        initHintPanel();
        initHyperlinksPanel();
        initFields();
    }

    private void initHeaderPanel() {
/*
        String title = isEmpty(workspace.getName())
                ? txt("app.shared.placeholder.Unnamed")
                : workspace.getName();
        headerPanel.add(new DBNHeaderForm(this, title, Icons.DB_LIQUIBASE).getComponent());
*/
    }

    private void initHintPanel() {
        TextContent hint = plain(txt("cfg.liquibase.hint.WorkspaceSettings"));
        hintPanel.add(new DBNHintForm(this, hint, null, true).getComponent());
    }

    private void initHyperlinksPanel() {
        Hyperlinks.initHyperlink(
                documentationLink,
                txt("app.liquibase.link.LiquibaseDocumentation"),
                "https://docs.liquibase.com/oss/reference-guide-4-33");
    }

    private void initFields() {
        initContentRoots();
        initDatabaseTypes();
        initChangelogFormats();
        initPlaceholders();
        resetFormChanges();
        initPathListeners();
        updatePathTooltips();
    }

    private void initContentRoots() {
        contentRootComboBox.setContentRoots(workspaces.getContentRoots());
    }

    private void initDatabaseTypes() {
        List<DatabaseType> values = getDatabaseTypeValues();
        databaseTypeSelector.setValues(values);
        setSelection(databaseTypeSelector, workspace.getDatabaseType());
        onSelectionChange(databaseTypeSelector, type -> updateMasterChangelogExtension(getSelection(changelogFormatSelector)));
    }

    private void initChangelogFormats() {
        changelogFormatSelector.setValues(List.of(LiquibaseChangelogFormat.values()));
        setSelection(changelogFormatSelector, workspace.getChangelogFormat());
        onSelectionChange(changelogFormatSelector, this::updateMasterChangelogExtension);
    }

    private void updateMasterChangelogExtension(@Nullable LiquibaseChangelogFormat format) {
        if (format == null) return;

        masterChangelogTextField.getEmptyText().setText(
                getDefaultMasterChangelog(format, getSelection(databaseTypeSelector)));

        String changelog = getText(masterChangelogTextField);
        if (isEmpty(changelog)) return;

        setText(masterChangelogTextField,
                LiquibaseChangelogFiles.normalize(changelog, format, getSelection(databaseTypeSelector)));
    }

    @NotNull
    private List<DatabaseType> getDatabaseTypeValues() {
        if (databaseType == null) return Arrays.asList(DatabaseType.supported());
        DatabaseType sourceType = databaseType == DatabaseType.UNKNOWN ? DatabaseType.GENERIC : databaseType;
        if (sourceType == DatabaseType.GENERIC) return List.of(DatabaseType.GENERIC);
        return List.of(DatabaseType.GENERIC, sourceType);
    }

    private void initPlaceholders() {
        rootPathTextField.getEmptyText().setText(DEFAULT_ROOT_PATH);
        changelogDirectoryTextField.getEmptyText().setText(DEFAULT_CHANGELOG_DIRECTORY);
        sqlDirectoryTextField.getEmptyText().setText(DEFAULT_SQL_DIRECTORY);
        documentationDirectoryTextField.getEmptyText().setText(DEFAULT_DOCUMENTATION_DIRECTORY);
        LiquibaseChangelogFormat changelogFormat = workspace.getChangelogFormat();
        DatabaseType databaseType = workspace.getDatabaseType();
        String defaultChangelog = getDefaultMasterChangelog(changelogFormat, databaseType);

        masterChangelogTextField.getEmptyText().setText(defaultChangelog);
        propertiesFileTextField.getEmptyText().setText(DEFAULT_PROPERTIES_FILE);
    }

    private void initPathListeners() {
        onTextChange(nameTextField, e -> updateWorkspaceName());
        onSelectionChange(contentRootComboBox, root -> updatePathTooltips());
        onTextChange(rootPathTextField, e -> updatePathTooltips());
        onTextChange(changelogDirectoryTextField, e -> updatePathTooltips());
        onTextChange(sqlDirectoryTextField, e -> updatePathTooltips());
        onTextChange(documentationDirectoryTextField, e -> updatePathTooltips());
        onTextChange(masterChangelogTextField, e -> updatePathTooltips());
        onTextChange(propertiesFileTextField, e -> updatePathTooltips());
    }

    private void updateWorkspaceName() {
        String oldName = workspace.getName();
        String newName = getText(nameTextField);
        if (getGeneratedRootPath(oldName).equals(getText(rootPathTextField))) {
            setText(rootPathTextField, getGeneratedRootPath(newName));
        }
        workspace.setName(newName);

        Disposable parent = ensureParentComponent();
        if (parent instanceof LiquibaseWorkspacesForm bundleForm) {
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
            setToolTipText(changelogDirectoryTextField, null);
            setToolTipText(sqlDirectoryTextField, null);
            setToolTipText(documentationDirectoryTextField, null);
            setToolTipText(masterChangelogTextField, null);
            setToolTipText(propertiesFileTextField, null);
            return;
        }
        String liquibaseRoot = appendPath(contentRoot, getText(rootPathTextField));
        rootPathInfoLabel.setText(liquibaseRoot);
        setToolTipText(rootPathTextField, liquibaseRoot);
        setToolTipText(changelogDirectoryTextField, appendPath(liquibaseRoot, getText(changelogDirectoryTextField)));
        setToolTipText(sqlDirectoryTextField, appendPath(liquibaseRoot, getText(sqlDirectoryTextField)));
        setToolTipText(documentationDirectoryTextField, appendPath(liquibaseRoot, getText(documentationDirectoryTextField)));
        setToolTipText(masterChangelogTextField, appendPath(liquibaseRoot, getText(masterChangelogTextField)));
        setToolTipText(propertiesFileTextField, appendPath(contentRoot, getText(propertiesFileTextField)));
    }

    private static String appendPath(String parent, String child) {
        if (parent == null || parent.isEmpty()) return child;
        if (child == null || child.isEmpty()) return parent;
        return parent + "/" + child;
    }

    @Override
    protected void initValidation() {
        addRequiredTextValidation(nameTextField, txt("msg.liquibase.error.WorkspaceNameRequired"));
        addSelectionValidation(databaseTypeSelector, txt("msg.liquibase.error.DatabaseTypeRequired"));
        addValidation(changelogFormatSelector, selector -> validateChangelogFormat());
        addValidation(nameTextField, field -> validateWorkspaceName());
        addSelectionValidation(contentRootComboBox,    txt("msg.liquibase.error.ContentRootRequired"));
        addValidation(rootPathTextField, field -> validateWorkspaceRoot());

        addRequiredTextValidation(rootPathTextField,           txt("msg.liquibase.error.RootPathRequired"));
        addRequiredTextValidation(changelogDirectoryTextField, txt("msg.liquibase.error.ChangelogDirectoryRequired"));
        addRequiredTextValidation(sqlDirectoryTextField,       txt("msg.liquibase.error.SqlDirectoryRequired"));
        addRequiredTextValidation(documentationDirectoryTextField, txt("msg.liquibase.error.DocumentationDirectoryRequired"));
        addRequiredTextValidation(masterChangelogTextField,    txt("msg.liquibase.error.MasterChangelogRequired"));
        addRequiredTextValidation(propertiesFileTextField,     txt("msg.liquibase.error.PropertiesFileRequired"));

        addTextValidation(rootPathTextField,           v -> isValidRelativePath(v), txt("msg.liquibase.error.InvalidDirectoryPath"));
        addTextValidation(changelogDirectoryTextField, v -> isValidRelativePath(v), txt("msg.liquibase.error.InvalidDirectoryPath"));
        addTextValidation(sqlDirectoryTextField,       v -> isValidRelativePath(v), txt("msg.liquibase.error.InvalidDirectoryPath"));
        addTextValidation(documentationDirectoryTextField, v -> isValidRelativePath(v), txt("msg.liquibase.error.InvalidDirectoryPath"));
        addTextValidation(masterChangelogTextField,    v -> isValidFileName(v),     txt("msg.liquibase.error.InvalidFileName"));
        addTextValidation(propertiesFileTextField,     v -> isValidFileName(v),     txt("msg.liquibase.error.InvalidFileName"));
    }

    private String validateWorkspaceRoot() {
        String selectedPath = contentRootComboBox.getSelectedPath();
        if (selectedPath == null) return null;

        LiquibaseWorkspace owner = workspaces.findRootOwner(selectedPath, getText(rootPathTextField), workspace);
        return owner == null ? null : txt("msg.liquibase.error.ContentRootAlreadyMapped", getWorkspaceName(owner));
    }

    private String validateChangelogFormat() {
        LiquibaseChangelogFormat format = getSelection(changelogFormatSelector);
        DatabaseType databaseType = getSelection(databaseTypeSelector);
        return format == LiquibaseChangelogFormat.SQL &&
                (databaseType == null || databaseType == DatabaseType.GENERIC || databaseType == DatabaseType.UNKNOWN)
                ? txt("msg.liquibase.error.ChangelogFormatRequiresDatabaseType") : null;
    }

    private String getWorkspaceName(LiquibaseWorkspace workspace) {
        return isEmpty(workspace.getName()) ? txt("app.shared.placeholder.Unnamed") : workspace.getName();
    }

    private String validateWorkspaceName() {
        LiquibaseWorkspace owner = workspaces.findNameOwner(getText(nameTextField), workspace);
        return owner == null ? null : txt("msg.liquibase.error.WorkspaceNameAlreadyUsed");
    }

    private boolean isValidRelativePath(String value) {
        try {
            Path path = Paths.get(value);
            return !path.isAbsolute() && path.getNameCount() > 0 &&
                    !".".equals(path.toString()) && !"..".equals(path.toString());
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

    public void resetFormChanges() {
        setText(nameTextField, workspace.getName());
        setSelection(databaseTypeSelector, workspace.getDatabaseType());
        setText(rootPathTextField, workspace.getRootPath());
        contentRootComboBox.setSelectedPath(workspace.getContentRootPath());
        setSelection(changelogFormatSelector, workspace.getChangelogFormat());
        setText(changelogDirectoryTextField, workspace.getChangelogDirectory());
        setText(sqlDirectoryTextField, workspace.getSqlDirectory());
        setText(documentationDirectoryTextField, workspace.getDocumentationDirectory());
        setText(masterChangelogTextField, workspace.getMasterChangelog());
        updateMasterChangelogExtension(workspace.getChangelogFormat());
        setText(propertiesFileTextField, workspace.getPropertiesFile());
    }

    public void applyFormChanges() {
        workspace.setName(getText(nameTextField));
        workspace.setDatabaseType(getSelection(databaseTypeSelector));
        workspace.setRootPath(getText(rootPathTextField));
        workspace.setContentRootPath(contentRootComboBox.getSelectedPath());
        workspace.setChangelogFormat(getSelection(changelogFormatSelector));
        workspace.setChangelogDirectory(getText(changelogDirectoryTextField));
        workspace.setSqlDirectory(getText(sqlDirectoryTextField));
        workspace.setDocumentationDirectory(getText(documentationDirectoryTextField));
        workspace.setMasterChangelog(getText(masterChangelogTextField));
        workspace.setPropertiesFile(getText(propertiesFileTextField));

        DatabaseType databaseType = workspace.getDatabaseType();
        if (databaseType != null) {
            Project project = ensureProject();
            FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
            Path rootPath = new LiquibaseWorkspacePaths(workspace).getLiquibaseRootPath();
            VirtualFile rootDirectory = LocalFileSystem.getInstance().findFileByPath(rootPath.toString());

            contextManager.setVirtualConnection(rootDirectory, databaseType);
        }
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
