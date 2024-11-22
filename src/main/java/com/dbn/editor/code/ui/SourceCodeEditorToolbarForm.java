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

package com.dbn.editor.code.ui;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNToolbarForm;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Actions;
import com.dbn.editor.code.SourceCodeEditor;
import com.dbn.editor.code.SourceCodeManagerListener;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.dbn.vfs.file.status.DBFileStatus;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class SourceCodeEditorToolbarForm extends DBNToolbarForm {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JPanel loadingDataPanel;
    private JLabel loadingLabel;
    private JPanel loadingIconPanel;

    private final WeakRef<SourceCodeEditor> sourceCodeEditor;

    public SourceCodeEditorToolbarForm(@NotNull SourceCodeEditor sourceCodeEditor) {
        super(sourceCodeEditor, sourceCodeEditor.getProject());
        this.mainPanel.setBorder(Borders.insetBorder(2));
        this.sourceCodeEditor = WeakRef.of(sourceCodeEditor);

        DBSourceCodeVirtualFile sourceCodeFile = sourceCodeEditor.getVirtualFile();

        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, "DBNavigator.ActionGroup.SourceEditor", "", true);
        this.actionsPanel.add(actionToolbar.getComponent(), BorderLayout.CENTER);
        this.loadingIconPanel.add(new AsyncProcessIcon("Loading"), BorderLayout.CENTER);
        this.loadingDataPanel.setVisible(sourceCodeFile.is(DBFileStatus.LOADING));

        ProjectEvents.subscribe(ensureProject(), this, SourceCodeManagerListener.TOPIC, sourceCodeManagerListener());
        Disposer.register(sourceCodeEditor, this);
    }

    @NotNull
    private SourceCodeManagerListener sourceCodeManagerListener() {
        return new SourceCodeManagerListener() {
            @Override
            public void sourceCodeLoading(@NotNull DBSourceCodeVirtualFile sourceCodeFile) {
                DBSourceCodeVirtualFile virtualFile = getVirtualFile();
                if (virtualFile.equals(sourceCodeFile)) {
                    Dispatch.run(() -> loadingDataPanel.setVisible(true));
                }
            }

            @Override
            public void sourceCodeLoaded(@NotNull DBSourceCodeVirtualFile sourceCodeFile, boolean initialLoad) {
                DBSourceCodeVirtualFile virtualFile = getVirtualFile();
                if (virtualFile.equals(sourceCodeFile)) {
                    Dispatch.run(() -> loadingDataPanel.setVisible(false));
                }
            }
        };
    }

    @NotNull
    private DBSourceCodeVirtualFile getVirtualFile() {
        return getSourceCodeEditor().getVirtualFile();
    }

    @NotNull
    public SourceCodeEditor getSourceCodeEditor() {
        return sourceCodeEditor.ensure();
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public Object getData(@NotNull String dataId) {
        if (PlatformDataKeys.VIRTUAL_FILE.is(dataId)) return getSourceCodeEditor().getVirtualFile();
        if (PlatformDataKeys.FILE_EDITOR.is(dataId))  return getSourceCodeEditor();
        if (PlatformDataKeys.EDITOR.is(dataId)) return getSourceCodeEditor().getEditor();

        return null;
    }

}
