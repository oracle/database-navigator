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
import com.dbn.common.ui.form.DBNToolbarForm;
import com.dbn.common.ui.panel.DBNLoadingPanel;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Actions;
import com.dbn.editor.code.SourceCodeEditor;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.dbn.vfs.file.status.DBFileStatus;
import com.dbn.vfs.file.status.DBFileStatusListener;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;

import static com.dbn.common.ui.panel.DBNLoadingPanel.newLoadingPanel;
import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.vfs.file.status.DBFileStatus.*;

public class SourceCodeEditorToolbarForm extends DBNToolbarForm {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JPanel loadingPanel;

    private final WeakRef<SourceCodeEditor> sourceCodeEditor;

    public SourceCodeEditorToolbarForm(@NotNull SourceCodeEditor sourceCodeEditor) {
        super(sourceCodeEditor, sourceCodeEditor.getProject());
        this.mainPanel.setBorder(Borders.insetBorder(2));
        this.sourceCodeEditor = WeakRef.of(sourceCodeEditor);

        DBSourceCodeVirtualFile sourceCodeFile = sourceCodeEditor.getVirtualFile();

        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, true, "DBN.SourceEditor");
        setAccessibleName(actionToolbar, txt("app.codeEditor.aria.SourceCodeEditorActions"));
        this.actionsPanel.add(actionToolbar.getComponent());

        initLoadingPanel();

        ProjectEvents.subscribe(ensureProject(), this, DBFileStatusListener.TOPIC, fileStatusListener());
        Disposer.register(sourceCodeEditor, this);
    }

    private void initLoadingPanel() {
        DBSourceCodeVirtualFile sourceCodeFile = getSourceCodeEditor().getVirtualFile();

        newLoadingPanel(this, txt("app.codeEditor.text.LoadingSource")).installOn(this.loadingPanel, sourceCodeFile.is(LOADING));
        this.loadingPanel.setBorder(Borders.tableBorder(1, 0, 0, 0));
    }

    @NotNull
    private DBFileStatusListener fileStatusListener() {
        return (sourceCodeFile, status, value) -> {
            if (status != LOADING) return;

            DBSourceCodeVirtualFile virtualFile = getVirtualFile();
            if (virtualFile.equals(sourceCodeFile)) {
                dispatch(() -> loadingPanel.setVisible(virtualFile.is(LOADING)));
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
