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

package com.dbn.editor.console.ui;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.ui.AutoCommitLabel;
import com.dbn.common.ui.form.DBNToolbarForm;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Actions;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.editor.console.SQLConsoleEditor;
import com.dbn.vfs.file.DBConsoleVirtualFile;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import java.awt.BorderLayout;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;

public class SQLConsoleEditorToolbarForm extends DBNToolbarForm {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private AutoCommitLabel autoCommitLabel;

    public SQLConsoleEditorToolbarForm(Project project, SQLConsoleEditor fileEditor) {
        super(fileEditor, project);
        this.mainPanel.setBorder(Borders.insetBorder(2));

        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, true, "DBNavigator.ActionGroup.FileEditor");
        setAccessibleName(actionToolbar, txt("app.codeEditor.aria.SqlConsoleEditorActions"));
        this.actionsPanel.add(actionToolbar.getComponent(), BorderLayout.CENTER);

        DBConsoleVirtualFile file = fileEditor.getVirtualFile();
        ConnectionHandler connection = file.getConnection();
        DatabaseSession session = file.getSession();
        this.autoCommitLabel.init(project, file, connection, session);
        Disposer.register(this, autoCommitLabel);
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        SQLConsoleEditor fileEditor = ensureParentComponent();
        if (PlatformDataKeys.VIRTUAL_FILE.is(dataId)) return fileEditor.getVirtualFile();
        if (PlatformDataKeys.FILE_EDITOR.is(dataId))  return fileEditor;
        if (PlatformDataKeys.EDITOR.is(dataId)) return fileEditor.getEditor();

        return null;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }
}
