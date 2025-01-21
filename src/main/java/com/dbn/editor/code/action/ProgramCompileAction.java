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

import com.dbn.connection.operation.options.OperationSettings;
import com.dbn.database.DatabaseFeature;
import com.dbn.editor.DBContentType;
import com.dbn.editor.code.SourceCodeEditor;
import com.dbn.execution.compiler.CompileType;
import com.dbn.execution.compiler.CompilerAction;
import com.dbn.execution.compiler.CompilerActionSource;
import com.dbn.execution.compiler.DatabaseCompilerManager;
import com.dbn.execution.compiler.options.CompilerSettings;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.property.DBObjectProperty;
import com.dbn.object.common.status.DBObjectStatus;
import com.dbn.object.common.status.DBObjectStatusHolder;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import static com.dbn.editor.DBContentType.*;
import static com.dbn.nls.NlsResources.txt;

public class ProgramCompileAction extends AbstractCodeEditorAction {

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull SourceCodeEditor fileEditor, @NotNull DBSourceCodeVirtualFile sourceCodeFile) {
        DatabaseCompilerManager compilerManager = DatabaseCompilerManager.getInstance(project);
        CompilerSettings compilerSettings = getCompilerSettings(project);
        DBContentType contentType = sourceCodeFile.getContentType();
        CompilerAction compilerAction = new CompilerAction(CompilerActionSource.COMPILE, contentType, sourceCodeFile, fileEditor);
        compilerManager.compileInBackground(sourceCodeFile.getObject(), compilerSettings.getCompileType(), compilerAction);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project, @Nullable SourceCodeEditor fileEditor, @Nullable DBSourceCodeVirtualFile sourceCodeFile) {
        Presentation presentation = e.getPresentation();
        if (sourceCodeFile == null) {
            presentation.setEnabled(false);
        } else {

            DBSchemaObject schemaObject = sourceCodeFile.getObject();
            if (schemaObject.is(DBObjectProperty.COMPILABLE) && DatabaseFeature.OBJECT_INVALIDATION.isSupported(schemaObject)) {
                CompilerSettings compilerSettings = getCompilerSettings(schemaObject.getProject());
                CompileType compileType = compilerSettings.getCompileType();
                DBObjectStatusHolder objectStatus = schemaObject.getStatus();
                DBContentType contentType = sourceCodeFile.getContentType();

                boolean isDebug = compileType == CompileType.DEBUG;
                if (compileType == CompileType.KEEP) {
                    isDebug = objectStatus.is(contentType, DBObjectStatus.DEBUG);
                }

                boolean isPresent = objectStatus.is(contentType, DBObjectStatus.PRESENT);
                boolean isValid = objectStatus.is(contentType, DBObjectStatus.VALID);
                boolean isModified = sourceCodeFile.isModified();

                boolean isCompiling = objectStatus.is(contentType, DBObjectStatus.COMPILING);
                boolean isEnabled = !isModified && isPresent && !isCompiling && (compilerSettings.isAlwaysShowCompilerControls() || !isValid /*|| isDebug != isDebugActive*/);

                presentation.setEnabled(isEnabled);
                String text =
                        contentType == CODE_SPEC ? txt("app.codeEditor.action.CompileSpec") :
                        contentType == CODE_BODY ? txt("app.codeEditor.action.CompileBody") :
                                txt("app.codeEditor.action.Compile");

                if (isDebug) text = text + " " + txt("app.codeEditor.action.DebugSuffix");
                if (compileType == CompileType.ASK) text = text + "...";

                presentation.setVisible(true);
                presentation.setText(text);

                Icon icon = isDebug ?
                        CompileType.DEBUG.getIcon() :
                        CompileType.NORMAL.getIcon();
                presentation.setIcon(icon);
            } else {
                presentation.setVisible(false);
            }
        }
    }

    private static CompilerSettings getCompilerSettings(Project project) {
        return OperationSettings.getInstance(project).getCompilerSettings();
    }
}
