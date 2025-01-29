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

package com.dbn.editor.code;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.editor.BasicTextEditorProvider;
import com.dbn.common.environment.EnvironmentManager;
import com.dbn.common.exception.ProcessDeferredException;
import com.dbn.common.util.Editors;
import com.dbn.editor.DBContentType;
import com.dbn.editor.DatabaseFileEditorManager;
import com.dbn.editor.EditorProviderId;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl;
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

import static com.dbn.common.compatibility.CompatibilityUtil.isStructureViewAccess;

abstract class SourceCodeEditorProviderBase extends BasicTextEditorProvider implements DumbAware {
    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        if (virtualFile instanceof DBSourceCodeVirtualFile) {
            // accept provider if invoked for a child file (ide invocations)
            //  => custom handling when createEditor(...) is invoked
            DBSourceCodeVirtualFile sourceCodeVirtualFile = (DBSourceCodeVirtualFile) virtualFile;
            DBContentType contentType = sourceCodeVirtualFile.getContentType();
            return contentType == getContentType();
        }
        return false;
    }


    @Override
    @NotNull
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        DBEditableObjectVirtualFile databaseFile;
        boolean temporary = false;

        if (file instanceof DBSourceCodeVirtualFile) {
            DBSourceCodeVirtualFile sourceCodeFile = (DBSourceCodeVirtualFile) file;
            databaseFile = sourceCodeFile.getMainDatabaseFile();

            temporary = isStructureViewAccess();
            if (temporary) return createBasicEditor(project, sourceCodeFile);

            // trigger main file editor and cancel the original request
            // (prevent creation of editor if invoked for the source code file -  e.g. debugger navigation / file bookmarks)
            openMainFileEditor(databaseFile);
        } else {
            databaseFile = (DBEditableObjectVirtualFile) file;
        }

        DBSourceCodeVirtualFile sourceCodeFile = databaseFile.ensureContentFile(getContentType());
        SourceCodeEditor sourceCodeEditor = createCodeEditor(project, sourceCodeFile);
        return prepareEditor(project, sourceCodeEditor, sourceCodeFile);
    }

    @NotNull
    private static PsiAwareTextEditorImpl createBasicEditor(@NotNull Project project, DBSourceCodeVirtualFile sourceCodeFile) {
        return new PsiAwareTextEditorImpl(project, sourceCodeFile, TextEditorProvider.getInstance());
    }

    @NotNull
    private SourceCodeEditor createCodeEditor(@NotNull Project project, DBSourceCodeVirtualFile sourceCodeFile) {
        DBEditableObjectVirtualFile databaseFile = sourceCodeFile.getMainDatabaseFile();
        String editorName = getName();
        boolean isMainEditor = sourceCodeFile.getContentType() == databaseFile.getMainContentType();
        EditorProviderId editorProviderId = getEditorProviderId();
        SourceCodeEditor sourceCodeEditor = isMainEditor ?
                new SourceCodeMainEditor(project, sourceCodeFile, editorName, editorProviderId) :
                new SourceCodeEditor(project, sourceCodeFile, editorName, editorProviderId);
        return sourceCodeEditor;
    }

    private SourceCodeEditor prepareEditor(@NotNull Project project, SourceCodeEditor sourceCodeEditor, DBSourceCodeVirtualFile sourceCodeFile) {
        Editor editor = sourceCodeEditor.getEditor();
        EnvironmentManager environmentManager = EnvironmentManager.getInstance(project);
        if (environmentManager.isReadonly(sourceCodeFile) || !sourceCodeFile.isLoaded()) {
            Editors.setEditorReadonly(editor, true);
        }


        Document document = editor.getDocument();
        int documentSignature = document.hashCode();
        if (document.hashCode() != sourceCodeFile.getDocumentSignature()) {
            document.addDocumentListener(sourceCodeFile);
            sourceCodeFile.setDocumentSignature(documentSignature);
        }

        Icon icon = getIcon();
        if (icon != null) {
            DBEditableObjectVirtualFile databaseFile = sourceCodeFile.getMainDatabaseFile();
            updateTabIcon(databaseFile, sourceCodeEditor, icon);
        }
        return sourceCodeEditor;
    }

    private void openMainFileEditor(DBEditableObjectVirtualFile databaseFile) {
        Project project = databaseFile.getProject();
        DBSchemaObject object = databaseFile.getObject();
        DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(project);
        editorManager.connectAndOpenEditor(object, getEditorProviderId(), false, true);
        throw new ProcessDeferredException();
    }

    @Override
    public VirtualFile getContentVirtualFile(VirtualFile virtualFile) {
        if (virtualFile instanceof DBEditableObjectVirtualFile) {
            DBEditableObjectVirtualFile objectVirtualFile = (DBEditableObjectVirtualFile) virtualFile;
            return objectVirtualFile.getContentFile(getContentType());
        }
        return super.getContentVirtualFile(virtualFile);
    }

    public abstract DBContentType getContentType();

    public abstract String getName();

    public abstract Icon getIcon();

    @Override
    public void disposeEditor(@NotNull FileEditor editor) {
        Disposer.dispose(editor);
    }
}
