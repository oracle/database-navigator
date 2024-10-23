package com.dbn.common.util;

import com.dbn.common.color.Colors;
import com.dbn.common.compatibility.Workaround;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.editor.BasicTextEditor;
import com.dbn.common.file.util.VirtualFiles;
import com.dbn.common.navigation.NavigationInstructions;
import com.dbn.common.routine.Consumer;
import com.dbn.common.thread.*;
import com.dbn.common.ui.form.DBNToolbarForm;
import com.dbn.connection.ConnectionHandler;
import com.dbn.data.editor.text.TextContentType;
import com.dbn.ddl.DDLFileAttachmentManager;
import com.dbn.editor.EditorProviderId;
import com.dbn.editor.code.SourceCodeEditor;
import com.dbn.editor.data.DatasetEditor;
import com.dbn.editor.ddl.DDLFileEditor;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.psi.PsiUtil;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.vfs.file.*;
import com.intellij.ide.highlighter.HighlighterFactory;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.impl.EditorWindowHolder;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.EditorNotifications;
import com.intellij.ui.TabbedPaneWrapper;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import com.intellij.util.ui.UIUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.dbn.browser.DatabaseBrowserUtils.markSkipBrowserAutoscroll;
import static com.dbn.browser.DatabaseBrowserUtils.unmarkSkipBrowserAutoscroll;
import static com.dbn.common.dispose.Checks.isValid;

@Slf4j
@UtilityClass
public class Editors {

    public static FileEditor selectEditor(@NotNull Project project, @Nullable FileEditor fileEditor, @NotNull VirtualFile file, EditorProviderId editorProviderId, NavigationInstructions instructions) {
        if (fileEditor != null) {
            if (fileEditor instanceof DDLFileEditor) {
                DDLFileAttachmentManager attachmentManager = DDLFileAttachmentManager.getInstance(project);
                DBSchemaObject editableObject = attachmentManager.getMappedObject(file);
                if (editableObject != null) {
                    file = editableObject.getVirtualFile();
                }
            }
            openFileEditor(project, file, instructions.isFocus());

            if (fileEditor instanceof BasicTextEditor) {
                BasicTextEditor<?> basicTextEditor = (BasicTextEditor<?>) fileEditor;
                editorProviderId = basicTextEditor.getEditorProviderId();
                selectEditor(project, file, editorProviderId);
            }
        } else if (editorProviderId != null) {
            DBEditableObjectVirtualFile objectFile = VirtualFiles.resolveObjectFile(project, file);

            if (isValid(objectFile)) {
                FileEditor[] fileEditors;
                if (instructions.isOpen()) {
                    fileEditors = openFileEditor(project, objectFile, instructions.isFocus());
                } else{
                    fileEditors = getFileEditors(project, objectFile);
                }

                if (fileEditors != null &&  fileEditors.length > 0) {
                    selectEditor(project, objectFile, editorProviderId);
                    fileEditor = findTextEditor(fileEditors, editorProviderId);
                }
            }
        } else if (file.isInLocalFileSystem()) {
            if (instructions.isOpen()) {
                fileEditor = Commons.firstOrNull(openFileEditor(project, file, instructions.isFocus()));
            } else {
                fileEditor = Commons.firstOrNull(getFileEditors(project, file));
            }
        }

        if (instructions.isFocus() && fileEditor != null) {
            focusEditor(fileEditor);
        }

        return fileEditor;
    }

    private static FileEditor[] getFileEditors(Project project, VirtualFile file) {
        return Dispatch.call(() -> {
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
            return fileEditorManager.getEditors(file);
        });
    }

    public static Collection<TabInfo> getEditorTabInfos(Project project, VirtualFile file) {
        Set<TabInfo> tabInfos = new HashSet<>();

        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        FileEditor[] allEditors = editorManager.getAllEditors(file);

        for (FileEditor fileEditor : allEditors) {
            EditorWindowHolder editorWindow = UIUtil.getParentOfType(EditorWindowHolder.class, fileEditor.getComponent());
            if (editorWindow == null) continue;

            JBTabsImpl editorTabs = UIUtil.getParentOfType(JBTabsImpl.class, (Component) editorWindow);
            if (editorTabs == null) continue;

            TabInfo tabInfo = editorTabs.getTabs().stream().filter(t -> t.getComponent() == editorWindow).findFirst().orElse(null);
            if (tabInfo == null) continue;

            tabInfos.add(tabInfo);
        }
        return tabInfos;
    }


    public static void setEditorProviderIcon(@NotNull Project project, @NotNull VirtualFile file, @NotNull FileEditor fileEditor, Icon icon) {
        JBTabsImpl tabs = getEditorTabComponent(project, file, fileEditor);
        if (tabs == null) return;

        TabInfo tabInfo = getEditorTabInfo(tabs, fileEditor.getComponent());
        if (tabInfo == null) return;

        tabInfo.setIcon(icon);
    }

    @Nullable
    private static JBTabsImpl getEditorTabComponent(@NotNull Project project, @NotNull VirtualFile file, FileEditor fileEditor) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        FileEditor selectedEditor = fileEditorManager.getSelectedEditor(file);
        if (selectedEditor == null) {
            if (file.isInLocalFileSystem()) {
                DDLFileAttachmentManager ddlFileAttachmentManager = DDLFileAttachmentManager.getInstance(project);
                DBSchemaObject schemaObject = ddlFileAttachmentManager.getMappedObject(file);
                if (schemaObject != null) {
                    DBEditableObjectVirtualFile objectVirtualFile = schemaObject.getEditableVirtualFile();
                    selectedEditor = fileEditorManager.getSelectedEditor(objectVirtualFile);
                }
            }
        }
        if (selectedEditor != null) {
            return UIUtil.getParentOfType(JBTabsImpl.class, selectedEditor.getComponent());
        }
        return null;
    }

    @Nullable
    private static TabInfo getEditorTabInfo(@NotNull JBTabsImpl tabs, JComponent editorComponent) {
        Component wrapperComponent = UIUtil.getParentOfType(TabbedPaneWrapper.TabWrapper.class, editorComponent);
        List<TabInfo> tabInfos = tabs.getTabs();
        for (TabInfo tabInfo : tabInfos) {
            if (tabInfo.getComponent() == wrapperComponent) {
                return tabInfo;
            }
        }
        return null;
    }

    @Nullable
    public static BasicTextEditor<?> getTextEditor(DBSourceCodeVirtualFile sourceCodeFile) {
        DBEditableObjectVirtualFile databaseFile = sourceCodeFile.getMainDatabaseFile();
        Project project = databaseFile.getProject();
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        FileEditor[] fileEditors = editorManager.getEditors(databaseFile);
        for (FileEditor fileEditor : fileEditors) {
            if (fileEditor instanceof BasicTextEditor) {
                BasicTextEditor<?> basicTextEditor = (BasicTextEditor<?>) fileEditor;
                VirtualFile file = FileDocumentManager.getInstance().getFile(basicTextEditor.getEditor().getDocument());
                if (Objects.equals(file, sourceCodeFile)) {
                    return basicTextEditor;
                }
            }
        }
        return null;
    }

    @Nullable
    public static Editor getEditor(FileEditor fileEditor) {
        Editor editor = null;
        if (fileEditor instanceof TextEditor) {
            TextEditor textEditor = (TextEditor) fileEditor;
            editor = textEditor.getEditor();
        } else if (fileEditor instanceof BasicTextEditor) {
            BasicTextEditor<?> textEditor = (BasicTextEditor<?>) fileEditor;
            editor = textEditor.getEditor();

        }
        return editor != null && !editor.isDisposed() ? editor : null;
    }

    public static FileEditor getFileEditor(@Nullable Editor editor) {
        if (editor == null) return null;

        Project project = editor.getProject();
        if (project == null) return null;

        return getFileEditor(project, e -> editor == getEditor(e));
    }

    public static void initEditorHighlighter(
            @NotNull Editor editor,
            @NotNull TextContentType contentType) {
        if (editor instanceof EditorEx) {
            EditorEx editorEx = (EditorEx) editor;
            SyntaxHighlighter syntaxHighlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(contentType.getFileType(), editor.getProject(), null);
            EditorColorsScheme colorsScheme = editor.getColorsScheme();
            EditorHighlighter highlighter = HighlighterFactory.createHighlighter(syntaxHighlighter, colorsScheme);
            editorEx.setHighlighter(highlighter);
        }
    }

    public static void initEditorHighlighter(
            @NotNull Editor editor,
            @NotNull DBLanguage language,
            @Nullable ConnectionHandler connection) {
        DBLanguageDialect languageDialect = connection == null ?
                        language.getMainLanguageDialect() :
                        connection.getLanguageDialect(language);

        initEditorHighlighter(editor, languageDialect);
    }

    public static void initEditorHighlighter(
            @NotNull Editor editor,
            @NotNull DBLanguage language,
            @NotNull DBObject object) {
        DBLanguageDialect languageDialect = object.getLanguageDialect(language);
        initEditorHighlighter(editor, languageDialect);
    }

    private static void initEditorHighlighter(Editor editor, DBLanguageDialect languageDialect) {
        if (editor instanceof EditorEx) {
            EditorEx editorEx = (EditorEx) editor;
            SyntaxHighlighter syntaxHighlighter = languageDialect.getSyntaxHighlighter();

            EditorColorsScheme colorsScheme = editorEx.getColorsScheme();
            EditorHighlighter highlighter = HighlighterFactory.createHighlighter(syntaxHighlighter, colorsScheme);
            editorEx.setHighlighter(highlighter);
        }
    }

    public static void setEditorReadonly(Editor editor, boolean readonly) {
        if (editor instanceof EditorEx) {
            EditorEx editorEx = (EditorEx) editor;
            editorEx.setViewer(readonly);
            EditorColorsScheme scheme = editor.getColorsScheme();
            Dispatch.run(true, () -> {
                Color background = readonly ?
                        Colors.getReadonlyEditorBackground() :
                        null; // Colors.getEditorBackground();

                Color caretRowBackground = readonly ?
                        Colors.getReadonlyEditorCaretRowBackground() :
                        null; // Colors.getEditorCaretRowBackground();

                editorEx.setBackgroundColor(background);
                scheme.setColor(EditorColors.CARET_ROW_COLOR, caretRowBackground);
            });
        }
    }

    public static void setEditorsReadonly(DBContentVirtualFile contentFile, boolean readonly) {
        Project project = Failsafe.nn(contentFile.getProject());

        if (contentFile instanceof DBSourceCodeVirtualFile) {
            DBSourceCodeVirtualFile sourceCodeFile = (DBSourceCodeVirtualFile) contentFile;
            for (SourceCodeEditor sourceCodeEditor: getFileEditors(project, SourceCodeEditor.class)) {
                DBSourceCodeVirtualFile file = sourceCodeEditor.getVirtualFile();
                if (file.equals(sourceCodeFile)) {
                    setEditorReadonly(sourceCodeEditor.getEditor(), readonly);
                }
            }
        } else if (contentFile instanceof DBDatasetVirtualFile) {
            DBDatasetVirtualFile datasetFile = (DBDatasetVirtualFile) contentFile;
            DBEditableObjectVirtualFile objectFile = datasetFile.getMainDatabaseFile();
            for (DatasetEditor datasetEditor : getFileEditors(project, DatasetEditor.class)) {
                if (Objects.equals(datasetEditor.getDatabaseFile(), objectFile)) {
                    datasetEditor.getEditorTable().cancelEditing();
                    datasetEditor.setEnvironmentReadonly(readonly);
                }
            }
        }
    }

    public static <T extends FileEditor> List<T> getFileEditors(Project project, Class<T> type) {
        return getFileEditors(project, e -> type.isAssignableFrom(e.getClass()));
    }

    public static <T extends FileEditor> List<T> getFileEditors(Project project, Predicate<FileEditor> filter) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        FileEditor[] allEditors = Read.call(fileEditorManager, m -> m.getAllEditors());
        return Arrays
                .stream(allEditors)
                .filter(filter)
                .map(e -> (T) e)
                .collect(Collectors.toList());
    }

    @Nullable
    public static <T extends FileEditor> T getFileEditor(Project project, Predicate<FileEditor> filter) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        FileEditor[] allEditors = Read.call(fileEditorManager, m -> m.getAllEditors());
        return Arrays
                .stream(allEditors)
                .filter(filter)
                .map(e -> (T) e)
                .findFirst()
                .orElse(null);
    }

    @Nullable
    public static BasicTextEditor<?> getTextEditor(DBConsoleVirtualFile consoleVirtualFile) {
        Project project = consoleVirtualFile.getProject();
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        FileEditor[] fileEditors = editorManager.getEditors(consoleVirtualFile);
        for (FileEditor fileEditor : fileEditors) {
            if (fileEditor instanceof BasicTextEditor) {
                BasicTextEditor<?> basicTextEditor = (BasicTextEditor<?>) fileEditor;
                VirtualFile file = FileDocumentManager.getInstance().getFile(basicTextEditor.getEditor().getDocument());
                if (file!= null && file.equals(consoleVirtualFile)) {
                    return basicTextEditor;
                }
            }
        }
        return null;
    }

    /**
     * get all open editors for a virtual file including the attached ddl files
     */
    public static List<FileEditor> getScriptFileEditors(Project project, VirtualFile file) {
        assert file.isInLocalFileSystem();

        List<FileEditor> scriptFileEditors = new ArrayList<>();
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        FileEditor[] fileEditors = editorManager.getAllEditors(file);
        for (FileEditor fileEditor : fileEditors) {
            if (fileEditor instanceof TextEditor) {
                TextEditor textEditor = (TextEditor) fileEditor;
                scriptFileEditors.add(textEditor);
            }
        }
        DDLFileAttachmentManager fileAttachmentManager = DDLFileAttachmentManager.getInstance(project);
        DBSchemaObject schemaObject = fileAttachmentManager.getMappedObject(file);
        if (schemaObject != null) {
            DBEditableObjectVirtualFile editableObjectFile = schemaObject.getEditableVirtualFile();
            fileEditors = editorManager.getAllEditors(editableObjectFile);
            for (FileEditor fileEditor : fileEditors) {
                if (fileEditor instanceof DDLFileEditor) {
                    DDLFileEditor ddlFileEditor = (DDLFileEditor) fileEditor;
                    Editor editor = ddlFileEditor.getEditor();
                    PsiFile psiFile = PsiUtil.getPsiFile(project, editor.getDocument());
                    if (psiFile != null && psiFile.getVirtualFile().equals(file)) {
                        scriptFileEditors.add(ddlFileEditor);
                    }
                }
            }
        }

        return scriptFileEditors;
    }

    public static Editor getSelectedEditor(Project project) {
        if (project == null) return null;

        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        FileEditor[] fileEditors = fileEditorManager.getSelectedEditors();
        if (fileEditors.length == 1) {
            if (fileEditors[0] instanceof BasicTextEditor) {
                BasicTextEditor<?> textEditor = (BasicTextEditor<?>) fileEditors[0];
                return textEditor.getEditor();
            }
        }
        return fileEditorManager.getSelectedTextEditor();
    }

    public static Editor getSelectedEditor(Project project, FileType fileType){
        Editor editor = Editors.getSelectedEditor(project);
        if (editor == null) return null;

        VirtualFile file = Documents.getVirtualFile(editor);
        if (file != null && file.getFileType().equals(fileType)) {
            return editor;
        }
        return null;
    }

    private static void focusEditor(@Nullable FileEditor fileEditor) {
        if (fileEditor == null) return;

        Editor editor = getEditor(fileEditor);
        focusEditor(editor);
    }
    public static void focusEditor(@Nullable Editor editor) {
        if (editor == null) return;

        Project project = editor.getProject();
        IdeFocusManager ideFocusManager = IdeFocusManager.getInstance(project);
        Dispatch.run(() -> ideFocusManager.requestFocus(editor.getContentComponent(), true));
    }

    public static VirtualFile getSelectedFile(Project project) {
        if (project == null) return null;

        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        FileEditor[] fileEditors = fileEditorManager.getSelectedEditors();
        if (fileEditors.length > 0) {
            if (fileEditors[0] instanceof DatasetEditor) {
                DatasetEditor datasetEditor = (DatasetEditor) fileEditors[0];
                return datasetEditor.getDatabaseFile();
            } else if (fileEditors[0] instanceof BasicTextEditor) {
                BasicTextEditor<?> basicTextEditor = (BasicTextEditor<?>) fileEditors[0];
                return basicTextEditor.getVirtualFile();
            }
        }

        Editor editor = fileEditorManager.getSelectedTextEditor();
        if (editor == null) return null;

        return Documents.getVirtualFile(editor);
    }

    public static Dimension calculatePreferredSize(Editor editor) {
        int maxLength = 0;

        Document document = editor.getDocument();
        for (int i=0; i< document.getLineCount(); i++) {
            int length = document.getLineEndOffset(i) - document.getLineStartOffset(i);
            if (length > maxLength) {
                maxLength = length;
            }
        }

        int charWidth = com.intellij.openapi.editor.ex.util.EditorUtil.getSpaceWidth(Font.PLAIN, editor);

        int width = (charWidth + 1) * maxLength; // mono spaced fonts here
        int height = (editor.getLineHeight()) * document.getLineCount();
        return new Dimension(width, height);
    }

    public static void releaseEditor(@Nullable Editor editor) {
        if (editor == null) return;

        Dispatch.run(true, () -> {
            EditorFactory editorFactory = EditorFactory.getInstance();
            editorFactory.releaseEditor(editor);
        });

    }

    public static EditorNotifications getNotifications(Project project) {
        return EditorNotifications.getInstance(Failsafe.nd(project));
    }

    public static void updateAllNotifications(@NotNull Project project) {
        updateNotifications(project, null);
    }

    public static void updateNotifications(@NotNull Project project, @Nullable VirtualFile file) {
        EditorNotifications notifications = getNotifications(project);
        if (file == null)
            notifications.updateAllNotifications(); else
            notifications.updateNotifications(file);
    }

    public static boolean isDdlFileEditor(FileEditor fileEditor) {
        return fileEditor instanceof DDLFileEditor;
    }

    public static boolean isMainEditor(Editor editor) {
        if (editor.getEditorKind() != EditorKind.MAIN_EDITOR) return false;
        return getFileEditor(editor) != null;
    }

    public static void addEditorToolbar(@NotNull FileEditor fileEditor, DBNToolbarForm toolbarForm) {
        Project project = toolbarForm.ensureProject();
        JComponent toolbarComponent = toolbarForm.getComponent();
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        editorManager.addTopComponent(fileEditor, toolbarComponent);
    }

    public static void closeFileEditors(Project project, VirtualFile file) {
        FileEditorManager editorManager = FileEditorManager.getInstance(project);
        if (!editorManager.isFileOpen(file)) return;

        editorManager.closeFile(file);
    }

    public static FileEditor[] openFileEditor(Project project, VirtualFile file, boolean focus) {
        AtomicReference<FileEditor[]> fileEditors = new AtomicReference<>();
        openFileEditor(project, file, focus, editors -> fileEditors.set(editors));
        return fileEditors.get();
    }

    public static void openFileEditor(Project project, VirtualFile file, boolean focus, @Nullable Consumer<FileEditor[]> callback) {
        DDLFileAttachmentManager attachmentManager = DDLFileAttachmentManager.getInstance(project);
        attachmentManager.warmUpAttachedDDLFiles(file);

        ThreadInfo threadInfo = ThreadInfo.copy();
        Dispatch.run(() -> ThreadMonitor.surround(project, threadInfo, ThreadProperty.EDITOR_LOAD, () -> {
            try {
                markSkipBrowserAutoscroll(file);
                FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                boolean wasOpen = fileEditorManager.isFileOpen(file);

                FileEditor[] fileEditors = fileEditorManager.openFile(file, focus);
                if (callback != null) callback.accept(fileEditors);

                if (!wasOpen) updateNotifications(project, file);
            } finally {
                unmarkSkipBrowserAutoscroll(file);
            }
        }));

    }

    public static EditorEx createEditor(Document document, Project project, @Nullable VirtualFile file, @NotNull FileType fileType) {
        EditorFactory editorFactory = EditorFactory.getInstance();

        return  file == null ?
                Unsafe.cast(editorFactory.createEditor(document, project, fileType, false)) :
                Unsafe.cast(editorFactory.createEditor(document, project, file, false));
    }


    @Nullable
    public static BasicTextEditor findTextEditor(FileEditor[] fileEditors, EditorProviderId editorProviderId) {
        for (FileEditor openFileEditor : fileEditors) {
            if (openFileEditor instanceof BasicTextEditor) {
                BasicTextEditor<?> basicTextEditor = (BasicTextEditor<?>) openFileEditor;
                if (Objects.equals(basicTextEditor.getEditorProviderId(), editorProviderId)) {
                    return basicTextEditor;
                }
            }
        }
        return null;
    }

    public static void selectEditor(Project project, VirtualFile file, @Nullable EditorProviderId editorProviderId) {
        if (editorProviderId == null) return;

        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        Dispatch.run(() -> fileEditorManager.setSelectedEditor(file, editorProviderId.getId()));
    }

    @Workaround
    public static void updateEditorPresentations(Project project, VirtualFile... files) {
        if (files == null || files.length == 0) return;

        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        for (VirtualFile file : files) {
            fileEditorManager.updateFilePresentation(file);
        }

    }
}
