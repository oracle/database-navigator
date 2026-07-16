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

package com.dbn.common.util;

import com.dbn.common.action.UserDataKeys;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Read;
import com.dbn.common.thread.Write;
import com.dbn.connection.ConnectionHandler;
import com.dbn.editor.code.content.GuardedBlockType;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.psi.PsiUtil;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.command.undo.UndoUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.DocumentBulkUpdateListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.FileContentUtil;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.dispose.Failsafe.nn;
import static com.dbn.common.util.GuardedBlocks.createGuardedBlock;
import static com.dbn.common.util.GuardedBlocks.removeGuardedBlocks;
import static com.dbn.common.util.Lists.forEach;
import static com.dbn.common.util.TimeUtil.isOlderThan;
import static com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction;
import static com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl.HARD_REF_TO_DOCUMENT_KEY;
import static com.intellij.openapi.util.text.StringUtil.convertLineSeparators;
import static java.util.concurrent.TimeUnit.SECONDS;

@UtilityClass
public class Documents {

    public static void touchDocument(Editor editor, boolean reparse) {
        Document document = editor.getDocument();

        // restart highlighting
        Project project = editor.getProject();
        if (!isValid(project)) return;

        PsiFile file = Documents.getFile(editor);
        if (!(file instanceof DBLanguagePsiFile dbLanguageFile)) return;

        DBLanguage dbLanguage = dbLanguageFile.getDBLanguage();
        if (dbLanguage != null) {
            ConnectionHandler connection = dbLanguageFile.getConnection();
            Editors.initEditorHighlighter(editor, dbLanguage, connection);
        }

        if (reparse) {
            ProjectEvents.notify(project,
                    DocumentBulkUpdateListener.TOPIC,
                    (listener) -> listener.updateStarted(document));

            List<VirtualFile> files = Collections.singletonList(file.getVirtualFile());
            FileContentUtil.reparseFiles(project, files, true);

            Background.run(() -> Read.run(() -> {
                CodeFoldingManager codeFoldingManager = CodeFoldingManager.getInstance(project);
                codeFoldingManager.updateFoldRegionsAsync(editor, false);
            }));
        }
        refreshEditorAnnotations(file);
    }

    public static void refreshEditorAnnotations(@Nullable List<Editor> editor) {
        forEach(editor, e -> refreshEditorAnnotations(e));
    }
    public static void refreshEditorAnnotations(@Nullable Editor editor) {
        if (editor == null) return;
        refreshEditorAnnotations(Documents.getFile(editor));
    }

    public static void refreshEditorAnnotations(@Nullable PsiFile psiFile) {
        if (psiFile == null) return;

        Long lastRefresh = psiFile.getUserData(UserDataKeys.LAST_ANNOTATION_REFRESH);
        if (lastRefresh != null && !isOlderThan(lastRefresh, 1, SECONDS)) return;

        psiFile.putUserData(UserDataKeys.LAST_ANNOTATION_REFRESH, System.currentTimeMillis());

        if (!psiFile.isValid()) return;

        Project project = psiFile.getProject();
        DaemonCodeAnalyzer daemonCodeAnalyzer = DaemonCodeAnalyzer.getInstance(project);
        Read.run(() -> daemonCodeAnalyzer.restart(psiFile));
    }

    public static Document createDocument(CharSequence text) {
        EditorFactory editorFactory = EditorFactory.getInstance();
        return editorFactory.createDocument(text);
    }

    public static Document ensureDocument(@NotNull PsiFile file) {
        return nn(getDocument(file));
    }

    @Nullable
    public static Document getDocument(@NotNull PsiFile file) {
        if (isNotValid(file)) return null;

        Project project = file.getProject();
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        return Read.call(documentManager, m -> m.getDocument(file));
    }

    public static Editor[] getEditors(Document document) {
        return EditorFactory.getInstance().getEditors(document);
    }

    @Nullable
    public static PsiFile getFile(@Nullable Editor editor) {
        if (isNotValid(editor)) return null;

        Project project = editor.getProject();
        if (isNotValid(project)) return null;

        Document document = editor.getDocument();
        return PsiUtil.getPsiFile(project, document);
    }

    @Nullable
    public static VirtualFile getVirtualFile(Editor editor) {
        if (editor instanceof EditorEx editorEx) {
            VirtualFile virtualFile = editorEx.getVirtualFile();
            if (virtualFile != null) return virtualFile;
        }
        Document document = editor.getDocument();
        return getVirtualFile(document);
    }

    @Nullable
    private static VirtualFile getVirtualFile(Document document) {
        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        return fileDocumentManager.getFile(document);
    }

    @NotNull
    public static Document ensureDocument(@NotNull VirtualFile file) {
        return nn(getDocument(file));
    }

    @Nullable
    public static Document getDocument(@NotNull VirtualFile file) {
        return Read.call(file, f -> {
            FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
            return fileDocumentManager.getDocument(f);
        });
    }



    @Nullable
    public static PsiFile getPsiFile(Editor editor) {
        if (isNotValid(editor)) return null;

        Project project = editor.getProject();
        if (isNotValid(project)) return null;

        VirtualFile file = getVirtualFile(editor);
        if (isNotValid(file)) return null;

        return getPsiFile(project, file);
    }


    @Nullable
    public static PsiFile getPsiFile(Project project, VirtualFile virtualFile) {
        Document document = getDocument(virtualFile);
        if (document != null) {
            PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
            return psiDocumentManager.getPsiFile(document);
        } else {
            return null;
        }
    }

    public static void setReadonly(Document document, Project project, boolean readonly) {
        Write.run(project, () -> {
            //document.setReadOnly(readonly);
            removeGuardedBlocks(document, GuardedBlockType.READONLY_DOCUMENT);
            if (readonly) createGuardedBlock(document, GuardedBlockType.READONLY_DOCUMENT, null, false);
        });
    }

    public static void setText(@NotNull Editor editor, CharSequence text, boolean format) {
        Write.run(() -> {
            Document document = editor.getDocument();
            changeText(document, text);

            Project project = nd(editor.getProject());
            PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
            documentManager.commitDocument(document);

            if (format) {
                formatContent(editor);
            }
        });

    }

    public static void formatContent(@NotNull Editor editor) {
        Project project = editor.getProject();
        if (isNotValid(project)) return;

        runWriteCommandAction(project, () -> {
            PsiFile psiFile = getPsiFile(editor);
            if (isNotValid(psiFile)) return;

            CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
            codeStyleManager.reformat(psiFile);
        });
    }

    /**
     * Replaces the document text inside a plain write action.
     * <p>
     * This overload does not register an undoable project command, so it is best suited for
     * internal, generated, preview, or temporary documents where the change should not appear
     * in the IDE undo stack.
     *
     * @param document document to update
     * @param text new document text; line separators are normalized before writing
     */
    public static void setText(@NotNull Document document, CharSequence text) {
        Write.run(() -> changeText(document, text));
    }

    /**
     * Replaces the document text inside a project-scoped write command.
     * <p>
     * Unlike {@link #setText(Document, CharSequence)}, this overload passes the project to
     * {@link Write#run(Project, Runnable)}, which wraps the write action in an IntelliJ command
     * and makes the text replacement available through the IDE undo stack.
     *
     * @param project project used to register the undoable command
     * @param document document to update
     * @param text new document text; line separators are normalized before writing
     */
    public static void setText(@NotNull Project project, @NotNull Document document, CharSequence text) {
        Write.run(project, () -> changeText(document, text));
    }

    /**
     * Replaces the document text and makes the replacement the undo baseline.
     * <p>
     * This is useful for embedded editors that load content from an external selection: users
     * should be able to undo edits made after the load, but not undo past the selected content.
     */
    public static void resetText(@NotNull Project project, @NotNull Document document, CharSequence text) {
        UndoUtil.enableUndoFor(document);
        UndoUtil.disableUndoIn(document, () -> setText(document, text));
    }

    /**
     * Replaces the editor text and makes the replacement the undo baseline.
     */
    public static void resetText(@NotNull Editor editor, CharSequence text, boolean format) {
        Document document = editor.getDocument();

        UndoUtil.enableUndoFor(document);
        UndoUtil.disableUndoIn(document, () -> setText(editor, text, format));
    }

    public static String getText(@NotNull Document document) {
        return Read.call(() -> document.getText());
    }

    private static void changeText(Document document, CharSequence text) {
        boolean readonly = !document.isWritable();
        try {
            text = convertLineSeparators(text.toString());
            document.setReadOnly(false);
            document.setText(text);
        } finally {
            document.setReadOnly(readonly);
        }
    }

    public static void saveDocument(@NotNull Document document) {
        Write.run(() -> {
            FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
            fileDocumentManager.saveDocument(document);
        });
    }

    public static void cacheDocuments(List<VirtualFile> files) {
        if (files == null || files.isEmpty()) return;
        files.forEach(f -> cacheDocument(f));
    }

    public static void cacheDocument(VirtualFile file) {
        Document document = getDocument(file);
        file.putUserData(HARD_REF_TO_DOCUMENT_KEY, document);
    }

    public static void onDocumentChanged(@NotNull Document document, Disposable parentDisposable, Consumer<DocumentEvent> consumer) {
        document.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                consumer.accept(event);
            }
        }, parentDisposable);
    }
}
