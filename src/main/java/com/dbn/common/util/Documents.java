package com.dbn.common.util;

import com.dbn.common.action.UserDataKeys;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.thread.Read;
import com.dbn.common.thread.Write;
import com.dbn.connection.ConnectionHandler;
import com.dbn.editor.code.content.GuardedBlockType;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.psi.PsiUtil;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.DocumentBulkUpdateListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.FileContentUtil;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.common.dispose.Failsafe.nn;
import static com.dbn.common.util.GuardedBlocks.createGuardedBlock;
import static com.dbn.common.util.GuardedBlocks.removeGuardedBlocks;
import static com.dbn.common.util.Lists.forEach;
import static com.dbn.common.util.TimeUtil.isOlderThan;
import static com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl.HARD_REF_TO_DOCUMENT_KEY;
import static java.util.concurrent.TimeUnit.SECONDS;

@UtilityClass
public class Documents {

    public static void touchDocument(Editor editor, boolean reparse) {
        Document document = editor.getDocument();

        // restart highlighting
        Project project = editor.getProject();
        if (!isValid(project)) return;

        PsiFile file = Documents.getFile(editor);
        if (!(file instanceof DBLanguagePsiFile)) return;

        DBLanguagePsiFile dbLanguageFile = (DBLanguagePsiFile) file;
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

            CodeFoldingManager codeFoldingManager = CodeFoldingManager.getInstance(project);
            codeFoldingManager.updateFoldRegionsAsync(editor, false);
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

    @NotNull
    public static Document ensureDocument(@NotNull PsiFile file) {
        return nn(getDocument(file));
    }

    @Nullable
    public static Document getDocument(@NotNull PsiFile file) {
        if (isNotValid(file)) return null;

        Project project = file.getProject();
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        return documentManager.getDocument(file);
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
        if (editor instanceof EditorEx) {
            EditorEx editorEx = (EditorEx) editor;
            return editorEx.getVirtualFile();
        }
        Document document = editor.getDocument();
        return getVirtualFile(document);
    }

    @Nullable
    private static VirtualFile getVirtualFile(Document document) {
        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        return fileDocumentManager.getFile(document);
    }

    @Nullable
    public static Document getDocument(@NotNull VirtualFile virtualFile) {
        return Read.call(virtualFile, f -> {
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

    public static void setText(@NotNull Document document, CharSequence text) {
        FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();
        VirtualFile file = fileDocumentManager.getFile(document);
        if (isNotValid(file)) return;

        Write.run(() -> {
            boolean isReadonly = !document.isWritable();
            try {
                document.setReadOnly(false);
                document.setText(text);
            } finally {
                document.setReadOnly(isReadonly);
            }

        });
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
}
