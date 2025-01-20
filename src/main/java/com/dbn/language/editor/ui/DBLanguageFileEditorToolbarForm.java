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

package com.dbn.language.editor.ui;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.ui.AutoCommitLabel;
import com.dbn.common.ui.form.DBNToolbarForm;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Actions;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.mapping.FileConnectionContextListener;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.dbn.connection.session.DatabaseSession;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Objects;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;

public class DBLanguageFileEditorToolbarForm extends DBNToolbarForm {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private AutoCommitLabel autoCommitLabel;

    private final ActionToolbar actionToolbar;

    public DBLanguageFileEditorToolbarForm(FileEditor fileEditor, Project project, VirtualFile file) {
        super(fileEditor, project);
        this.mainPanel.setBorder(Borders.insetBorder(2));

        this.actionToolbar = Actions.createActionToolbar(actionsPanel, true, "DBNavigator.ActionGroup.FileEditor");
        setAccessibleName(actionToolbar, txt("app.codeEditor.aria.CodeEditorActions"));
        this.actionsPanel.add(actionToolbar.getComponent(), BorderLayout.CENTER);
        this.actionToolbar.getComponent().addComponentListener(createResizeListener());

        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
        ConnectionHandler connection = contextManager.getConnection(file);
        DatabaseSession databaseSession = contextManager.getDatabaseSession(file);

        this.autoCommitLabel.init(project, file, connection, databaseSession);
        Disposer.register(this, autoCommitLabel);

        ProjectEvents.subscribe(project, this, FileConnectionContextListener.TOPIC, createConnectionChangeListener(file));
    }

    @NotNull
    private ComponentListener createResizeListener() {
        return new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Dimension size = actionsPanel.getSize();
                Dimension preferredSize = actionToolbar.getComponent().getPreferredSize();
                if (size.getWidth() == preferredSize.getWidth()) return;
                actionsPanel.setSize(preferredSize);
            }
        };
    }

    @NotNull
    private FileConnectionContextListener createConnectionChangeListener(VirtualFile file) {
        return new FileConnectionContextListener() {
            @Override
            public void connectionChanged(Project project, VirtualFile virtualFile, ConnectionHandler connection) {
                if (Objects.equals(file, virtualFile)) {
                    actionToolbar.updateActionsImmediately();
                }
            }
        };
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        FileEditor fileEditor = ensureParentComponent();
        if (fileEditor instanceof TextEditor) {
            TextEditor textEditor = (TextEditor) fileEditor;
            if (PlatformDataKeys.VIRTUAL_FILE.is(dataId)) return textEditor.getFile();
            if (PlatformDataKeys.FILE_EDITOR.is(dataId))  return textEditor;
            if (PlatformDataKeys.EDITOR.is(dataId)) return textEditor.getEditor();
        }

        return null;
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public AutoCommitLabel getAutoCommitLabel() {
        return autoCommitLabel;
    }
}
