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

package com.dbn.generator.statement.action;

import com.dbn.common.action.ProjectAction;
import com.dbn.common.thread.Command;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionAction;
import com.dbn.connection.context.DatabaseContextBase;
import com.dbn.generator.statement.StatementGeneratorResult;
import com.dbn.language.common.psi.PsiUtil;
import com.dbn.language.sql.SQLFileType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.StringSelection;

import static com.dbn.nls.NlsResources.txt;

public abstract class GenerateStatementAction extends ProjectAction implements DatabaseContextBase {
    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        ConnectionAction.invoke(txt("msg.codeGenerator.title.GeneratingStatement"), false, this,
                action -> Progress.prompt(project, getConnection(), true,
                        txt("prc.codeGenerator.title.GeneratingStatement"),
                        txt("prc.codeGenerator.text.GeneratingStatement",e.getPresentation().getText()),
                        progress -> {
                            StatementGeneratorResult result = generateStatement(project);
                            if (result.getMessages().hasErrors()) {
                                Messages.showErrorDialog(project,
                                        txt("msg.codeGenerator.message.StatementGenerationError"),
                                        result.getMessages());
                            } else {
                                pasteStatement(result, project);
                            }
                        }));
    }


    private void pasteStatement(StatementGeneratorResult result, Project project) {
        Dispatch.run(() -> {
            Editor editor = Editors.getSelectedEditor(project, SQLFileType.INSTANCE);
            if (editor != null)
                pasteToEditor(editor, result); else
                pasteToClipboard(result, project);
        });
    }

    private static void pasteToClipboard(StatementGeneratorResult result, Project project) {
        StringSelection content = new StringSelection(result.getStatement());

        CopyPasteManager copyPasteManager = CopyPasteManager.getInstance();
        copyPasteManager.setContents(content);
        Messages.showInfoDialog(project,
                txt("msg.codeGenerator.title.StatementGenerated"),
                txt("msg.codeGenerator.message.StatementGeneratedToClipboard"));
    }

    private static void pasteToEditor(final Editor editor, final StatementGeneratorResult generatorResult) {
        Command.run(
                editor.getProject(),
                txt("prc.codeGenerator.label.ExtractStatement"),
                () -> {
                    String statement = generatorResult.getStatement();
                    PsiUtil.moveCaretOutsideExecutable(editor);
                    int offset = EditorModificationUtil.insertStringAtCaret(editor, statement + "\n\n", false, true);
                    offset = offset - statement.length() - 2;
                    /*editor.getMarkupModel().addRangeHighlighter(offset, offset + statement.length(),
                            HighlighterLayer.SELECTION,
                            EditorColorsManager.getInstance().getGlobalScheme().getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES),
                            HighlighterTargetArea.EXACT_RANGE);*/
                    editor.getSelectionModel().setSelection(offset, offset + statement.length());
                    editor.getCaretModel().moveToOffset(offset);

                });
    }

    protected abstract StatementGeneratorResult generateStatement(Project project);
}
