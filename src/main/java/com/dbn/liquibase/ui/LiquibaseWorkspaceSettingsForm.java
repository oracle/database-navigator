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

package com.dbn.liquibase.ui;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.info.DBNCommentLabel;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.ui.misc.ContentRootSelector;
import com.dbn.liquibase.model.LiquibaseWorkspace;
import com.dbn.liquibase.model.LiquibaseWorkspaceBundle;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.dbn.common.text.TextContent.plain;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.ui.util.Tooltips.setToolTipText;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.liquibase.model.LiquibaseWorkspace.DEFAULT_CHANGELOG_DIRECTORY;
import static com.dbn.liquibase.model.LiquibaseWorkspace.DEFAULT_MASTER_CHANGELOG;
import static com.dbn.liquibase.model.LiquibaseWorkspace.DEFAULT_PROPERTIES_FILE;
import static com.dbn.liquibase.model.LiquibaseWorkspace.DEFAULT_ROOT_PATH;
import static com.dbn.liquibase.model.LiquibaseWorkspace.DEFAULT_SQL_DIRECTORY;
import static com.dbn.nls.NlsResources.txt;

public class LiquibaseWorkspaceSettingsForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel hintPanel;
    private DBNHyperlinkLabel documentationLink;
    private ContentRootSelector contentRootComboBox;
    private JBTextField nameTextField;
    private JBTextField rootPathTextField;
    private JBTextField changelogDirectoryTextField;
    private JBTextField sqlDirectoryTextField;
    private JBTextField masterChangelogTextField;
    private JBTextField propertiesFileTextField;
    private DBNCommentLabel rootPathInfoLabel;

    private final LiquibaseWorkspaceBundle workspaces;
    private final LiquibaseWorkspace workspace;

    LiquibaseWorkspaceSettingsForm(@NotNull LiquibaseWorkspaceSettingsDialog parent) {
        this(parent,
                parent.getWorkspaces(),
                parent.getWorkspace());
    }

    LiquibaseWorkspaceSettingsForm(
            @NotNull DBNComponent parent,
            @NotNull LiquibaseWorkspaceBundle workspaces,
            @NotNull LiquibaseWorkspace workspace) {
        super(parent);
        this.workspaces = workspaces;
        this.workspace = workspace;
        initHeaderPanel();
        initHintPanel();
        initHyperlinksPanel();
        initFields();
    }

    private void initHeaderPanel() {
/*
        String title = isEmpty(artifact.getName())
                ? txt("app.shared.placeholder.Unnamed")
                : artifact.getName();
        headerPanel.add(new DBNHeaderForm(this, title, Icons.DB_LIQUIBASE).getComponent());
*/
    }

    private void initHintPanel() {
        TextContent hint = plain(txt("cfg.liquibase.hint.WorkspaceSettings"));
        hintPanel.add(new DBNHintForm(this, hint, null, true).getComponent());
    }

    private void initHyperlinksPanel() {
        documentationLink.setHyperlinkText(txt("cfg.liquibase.link.LiquibaseDocumentation"));
        documentationLink.setHyperlinkTarget("https://docs.liquibase.com/oss/reference-guide-4-33");
    }

    private void initFields() {
        initContentRoots();
        initPlaceholders();
        resetFormChanges();
        initPathListeners();
        updatePathTooltips();
    }

    private void initContentRoots() {
        contentRootComboBox.setContentRoots(workspaces.getContentRoots());
    }

    private void initPlaceholders() {
        rootPathTextField.getEmptyText().setText(DEFAULT_ROOT_PATH);
        changelogDirectoryTextField.getEmptyText().setText(DEFAULT_CHANGELOG_DIRECTORY);
        sqlDirectoryTextField.getEmptyText().setText(DEFAULT_SQL_DIRECTORY);
        masterChangelogTextField.getEmptyText().setText(DEFAULT_MASTER_CHANGELOG);
        propertiesFileTextField.getEmptyText().setText(DEFAULT_PROPERTIES_FILE);
    }

    private void initPathListeners() {
        onSelectionChange(contentRootComboBox, root -> updatePathTooltips());
        onTextChange(rootPathTextField, e -> updatePathTooltips());
        onTextChange(changelogDirectoryTextField, e -> updatePathTooltips());
        onTextChange(sqlDirectoryTextField, e -> updatePathTooltips());
        onTextChange(masterChangelogTextField, e -> updatePathTooltips());
        onTextChange(propertiesFileTextField, e -> updatePathTooltips());
    }

    private void updatePathTooltips() {
        String contentRoot = contentRootComboBox.getSelectedPath();
        if (isEmpty(contentRoot)) {
            rootPathInfoLabel.setText("");
            setToolTipText(rootPathTextField, null);
            setToolTipText(changelogDirectoryTextField, null);
            setToolTipText(sqlDirectoryTextField, null);
            setToolTipText(masterChangelogTextField, null);
            setToolTipText(propertiesFileTextField, null);
            return;
        }
        String liquibaseRoot = appendPath(contentRoot, getText(rootPathTextField));
        rootPathInfoLabel.setText(liquibaseRoot);
        setToolTipText(rootPathTextField, liquibaseRoot);
        setToolTipText(changelogDirectoryTextField, appendPath(liquibaseRoot, getText(changelogDirectoryTextField)));
        setToolTipText(sqlDirectoryTextField, appendPath(liquibaseRoot, getText(sqlDirectoryTextField)));
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
        addRequiredTextValidation(nameTextField, txt("msg.liquibase.error.ArtifactNameRequired"));
        addValidation(nameTextField, field -> validateWorkspaceName());
        addSelectionValidation(contentRootComboBox,    txt("msg.liquibase.error.ContentRootRequired"));
        addValidation(rootPathTextField, field -> validateWorkspaceRoot());

        addRequiredTextValidation(rootPathTextField,           txt("msg.liquibase.error.RootPathRequired"));
        addRequiredTextValidation(changelogDirectoryTextField, txt("msg.liquibase.error.ChangelogDirectoryRequired"));
        addRequiredTextValidation(sqlDirectoryTextField,       txt("msg.liquibase.error.SqlDirectoryRequired"));
        addRequiredTextValidation(masterChangelogTextField,    txt("msg.liquibase.error.MasterChangelogRequired"));
        addRequiredTextValidation(propertiesFileTextField,     txt("msg.liquibase.error.PropertiesFileRequired"));

        addTextValidation(rootPathTextField,           v -> isValidRelativePath(v), txt("msg.liquibase.error.InvalidDirectoryPath"));
        addTextValidation(changelogDirectoryTextField, v -> isValidRelativePath(v), txt("msg.liquibase.error.InvalidDirectoryPath"));
        addTextValidation(sqlDirectoryTextField,       v -> isValidRelativePath(v), txt("msg.liquibase.error.InvalidDirectoryPath"));
        addTextValidation(masterChangelogTextField,    v -> isValidFileName(v),     txt("msg.liquibase.error.InvalidFileName"));
        addTextValidation(propertiesFileTextField,     v -> isValidFileName(v),     txt("msg.liquibase.error.InvalidFileName"));
    }

    private String validateWorkspaceRoot() {
        String selectedPath = contentRootComboBox.getSelectedPath();
        if (selectedPath == null) return null;

        LiquibaseWorkspace owner = workspaces.findRootOwner(selectedPath, getText(rootPathTextField), workspace);
        return owner == null ? null : txt("msg.liquibase.error.ContentRootAlreadyMapped", getWorkspaceName(owner));
    }

    private String getWorkspaceName(LiquibaseWorkspace workspace) {
        return isEmpty(workspace.getName()) ? txt("app.shared.placeholder.Unnamed") : workspace.getName();
    }

    private String validateWorkspaceName() {
        LiquibaseWorkspace owner = workspaces.findNameOwner(getText(nameTextField), workspace);
        return owner == null ? null : txt("msg.liquibase.error.ArtifactNameAlreadyUsed");
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
        setText(rootPathTextField, workspace.getRootPath());
        contentRootComboBox.setSelectedPath(workspace.getContentRootPath());
        setText(changelogDirectoryTextField, workspace.getChangelogDirectory());
        setText(sqlDirectoryTextField, workspace.getSqlDirectory());
        setText(masterChangelogTextField, workspace.getMasterChangelog());
        setText(propertiesFileTextField, workspace.getPropertiesFile());
    }

    public void applyFormChanges() {
        workspace.setName(getText(nameTextField));
        workspace.setRootPath(getText(rootPathTextField));
        workspace.setContentRootPath(contentRootComboBox.getSelectedPath());
        workspace.setChangelogDirectory(getText(changelogDirectoryTextField));
        workspace.setSqlDirectory(getText(sqlDirectoryTextField));
        workspace.setMasterChangelog(getText(masterChangelogTextField));
        workspace.setPropertiesFile(getText(propertiesFileTextField));
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
