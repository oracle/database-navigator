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

package com.dbn.editor.code.action;

import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.Lookups;
import com.dbn.common.action.ProjectPopupAction;
import com.dbn.common.icon.Icons;
import com.dbn.ddl.action.DDLFileAttachAction;
import com.dbn.ddl.action.DDLFileCreateAction;
import com.dbn.ddl.action.DDLFileDetachAction;
import com.dbn.ddl.action.DDLFileSettingsAction;
import com.dbn.editor.DBContentType;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.type.DBObjectType;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

@BackgroundUpdate
public class CodeEditorDDLFileAction extends ProjectPopupAction {
    public CodeEditorDDLFileAction() {
        super(txt("app.codeEditor.action.CodeEditorDdlFiles"));
    }

    @Override
    public void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setIcon(Icons.CODE_EDITOR_DDL_FILE);
        presentation.setText(txt("app.codeEditor.action.DdlFiles"));

        boolean supported = isSupported(e);
        presentation.setVisible(supported);
        presentation.setEnabled(supported);
    }

    @Override
    public AnAction[] getChildren(AnActionEvent e) {
        if (!isSupported(e)) return AnAction.EMPTY_ARRAY;

        DBSourceCodeVirtualFile sourceCodeFile = getSourcecodeFile(e);
        if (sourceCodeFile == null) return AnAction.EMPTY_ARRAY;

        DBSchemaObject object = sourceCodeFile.getObject();
        return new AnAction[]{
                new DDLFileCreateAction(object),
                new DDLFileAttachAction(object),
                new DDLFileDetachAction(object),
                Separator.getInstance(),
                new DDLFileSettingsAction()
        };
    }

    private static boolean isSupported(AnActionEvent e) {
        DBSourceCodeVirtualFile sourceCodeFile = getSourcecodeFile(e);
        if (sourceCodeFile == null) return false;

        DBSchemaObject object = sourceCodeFile.getObject();
        DBContentType contentType = sourceCodeFile.getContentType();
        DBObjectType objectType = object.getObjectType();

        return objectType.supportsDdl(contentType);
    }

    protected static DBSourceCodeVirtualFile getSourcecodeFile(AnActionEvent e) {
        VirtualFile virtualFile = Lookups.getVirtualFile(e);
        return virtualFile instanceof DBSourceCodeVirtualFile ? (DBSourceCodeVirtualFile) virtualFile : null;
    }

}
