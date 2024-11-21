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

package com.dbn.editor;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.file.VirtualFileInfo;
import com.dbn.common.icon.OverlaidIcons;
import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.navigation.NavigationInstructions;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.thread.ThreadInfo;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.ddl.DDLFileAttachmentManager;
import com.dbn.ddl.options.DDLFileGeneralSettings;
import com.dbn.ddl.options.DDLFileSettings;
import com.dbn.editor.code.SourceCodeMainEditor;
import com.dbn.editor.code.SourceCodeManager;
import com.dbn.editor.data.filter.DatasetFilter;
import com.dbn.editor.data.filter.DatasetFilterManager;
import com.dbn.editor.data.filter.DatasetFilterType;
import com.dbn.editor.data.options.DataEditorSettings;
import com.dbn.object.DBConsole;
import com.dbn.object.DBDataset;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.property.DBObjectProperty;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.vfs.DatabaseFileManager;
import com.dbn.vfs.DatabaseFileSystem;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.dbn.vfs.file.DBFileOpenHandle;
import com.dbn.vfs.file.DBObjectVirtualFile;
import com.dbn.vfs.file.status.DBFileStatus;
import com.dbn.vfs.file.status.DBFileStatusListener;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.tabs.TabInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Collection;
import java.util.List;

import static com.dbn.browser.DatabaseBrowserUtils.markSkipBrowserAutoscroll;
import static com.dbn.browser.DatabaseBrowserUtils.unmarkSkipBrowserAutoscroll;
import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.dispose.Checks.allValid;
import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.navigation.NavigationInstruction.FOCUS;
import static com.dbn.common.navigation.NavigationInstruction.OPEN;
import static com.dbn.common.navigation.NavigationInstruction.SCROLL;
import static com.dbn.common.thread.ThreadProperty.DEBUGGER_NAVIGATION;
import static com.dbn.common.thread.ThreadProperty.WORKSPACE_RESTORE;
import static com.dbn.common.util.Conditional.when;
import static com.dbn.common.util.Editors.getEditorTabInfos;
import static com.dbn.editor.DatabaseFileEditorManager.COMPONENT_NAME;
import static com.dbn.vfs.DatabaseFileSystem.isFileOpened;

@State(
        name = COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DatabaseFileEditorManager extends ProjectComponentBase {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseFileEditorManager";

    public static DatabaseFileEditorManager getInstance(Project project) {
        return projectService(project, DatabaseFileEditorManager.class);
    }

    public DatabaseFileEditorManager(Project project) {
        super(project, COMPONENT_NAME);

        ProjectEvents.subscribe(project, this,
                DBFileStatusListener.TOPIC,
                createFileStatusListener());
    }

    @NotNull
    private DBFileStatusListener createFileStatusListener() {
        return (file, status, value) -> {
            if (status != DBFileStatus.MODIFIED) return;
            markEditorsModified(getProject(), file.getMainDatabaseFile(), value);
        };
    }

    private static void markEditorsModified(@NotNull Project project, @NotNull DBObjectVirtualFile file, boolean modified) {
        Dispatch.run(() -> {
            Collection<TabInfo> tabInfos = getEditorTabInfos(project, file);
            Icon icon = modified ? OverlaidIcons.addModifiedOverlay(file.getIcon()) : file.getIcon();
            for (TabInfo tabInfo : tabInfos) {
                tabInfo.setIcon(icon);
            }
        });
    }

    public boolean isFileOpen(DBSchemaObject object) {
        return isFileOpen(object.getEditableVirtualFile());
    }


    public boolean isFileOpen(DBEditableObjectVirtualFile databaseFile) {
        FileEditorManager editorManager = FileEditorManager.getInstance(getProject());
        return editorManager.isFileOpen(databaseFile);
    }

    public void connectAndOpenEditor(@NotNull DBObject object, @Nullable EditorProviderId editorProviderId, boolean scrollBrowser, boolean focusEditor) {
        if (!isEditable(object)) return;

        ConnectionAction.invoke("opening the object editor", false, object, action -> {
            if (focusEditor) {
                Project project = object.getProject();
                Progress.prompt(project, object, true,
                        "Opening " + object.getTypeName() + " editor",
                        "Opening editor for " + object.getQualifiedNameWithType(),
                        progress -> openEditor(object, editorProviderId, scrollBrowser, true));
            } else {
                Background.run(getProject(), () -> openEditor(object, editorProviderId, scrollBrowser, false));
            }
        });
    }

    public void openEditor(@NotNull DBObject object, @Nullable EditorProviderId editorProviderId, boolean scrollBrowser, boolean focusEditor) {
        if (!isEditable(object)) return;
        if (DBFileOpenHandle.isFileOpening(object)) return;

        NavigationInstructions editorInstructions = NavigationInstructions.create().with(OPEN).with(SCROLL, focusEditor).with(FOCUS, focusEditor);
        NavigationInstructions browserInstructions = NavigationInstructions.create().with(SCROLL, scrollBrowser);
        DBFileOpenHandle handle = DBFileOpenHandle.create(object).
                withEditorProviderId(editorProviderId).
                withEditorInstructions(editorInstructions).
                withBrowserInstructions(browserInstructions);

        try {
            handle.init();
            if (object.is(DBObjectProperty.SCHEMA_OBJECT)) {
                openSchemaObject(handle);

            } else {
                DBObject parentObject = object.getParentObject();
                if (parentObject.is(DBObjectProperty.SCHEMA_OBJECT)) {
                    openChildObject(handle);
                }
            }
        } finally {
            handle.release();
        }
    }

    private void openSchemaObject(@NotNull DBFileOpenHandle handle) {
        DBSchemaObject object = handle.getObject();
        object.makeEditorReady();

        DBEditableObjectVirtualFile databaseFile = getFileSystem().findOrCreateDatabaseFile(object);
        if (isNotValid(databaseFile)) return;

        EditorProviderId editorProviderId = handle.getEditorProviderId();
        databaseFile.setSelectedEditorProviderId(editorProviderId);

        invokeFileOpen(handle, () -> {
            if (!allValid(object, databaseFile)) return;


            // open / reopen (select) the file
            if (isFileOpened(object))
                openOrFocusEditor(handle, databaseFile, editorProviderId); else
                prepareEditor(databaseFile, () -> openOrFocusEditor(handle, databaseFile, editorProviderId));

        });
    }

    private void openChildObject(DBFileOpenHandle handle) {
        DBObject object = handle.getObject();
        DBSchemaObject schemaObject = object.getParentObject();
        schemaObject.makeEditorReady();

        DBEditableObjectVirtualFile databaseFile = getFileSystem().findOrCreateDatabaseFile(schemaObject);
        if (isNotValid(databaseFile)) return;

        Project project = schemaObject.getProject();
        SourceCodeManager sourceCodeManager = SourceCodeManager.getInstance(project);
        sourceCodeManager.ensureSourcesLoaded(schemaObject, false);

        invokeFileOpen(handle, () -> {
            if (isNotValid(schemaObject)) return;

            // open / reopen (select) the file
            if (isFileOpened(schemaObject))
                openOrFocusSelectEditor(handle, databaseFile); else
                prepareEditor(databaseFile, () -> openOrFocusSelectEditor(handle, databaseFile));

        });
    }

    private void openOrFocusEditor(@NotNull DBFileOpenHandle handle, DBEditableObjectVirtualFile databaseFile, EditorProviderId editorProviderId) {
        Project project = databaseFile.getProject();
        boolean focusEditor = handle.getEditorInstructions().isFocus();

        Editors.openFileEditor(project, databaseFile, focusEditor);
        NavigationInstructions instructions = NavigationInstructions.create().
                with(SCROLL).
                with(FOCUS, focusEditor);
        Editors.selectEditor(project, null, databaseFile, editorProviderId, instructions);
    }

    private void openOrFocusSelectEditor(DBFileOpenHandle handle, DBEditableObjectVirtualFile databaseFile) {
        Project project = databaseFile.getProject();
        boolean focusEditor = handle.getEditorInstructions().isFocus();
        Editors.openFileEditor(project, databaseFile, focusEditor, fileEditors ->
                focusEditor(handle, fileEditors, focusEditor, databaseFile));
    }

    private void focusEditor(DBFileOpenHandle handle, FileEditor[] fileEditors, boolean focusEditor, DBEditableObjectVirtualFile databaseFile) {
        DBObject object = handle.getObject();
        Project project = object.getProject();
        for (FileEditor fileEditor : fileEditors) {
            if (fileEditor instanceof SourceCodeMainEditor) {
                SourceCodeMainEditor sourceCodeEditor = (SourceCodeMainEditor) fileEditor;
                NavigationInstructions instructions = NavigationInstructions.create().
                        with(SCROLL).
                        with(FOCUS, focusEditor);

                EditorProviderId editorProviderId = handle.getEditorProviderId();
                Editors.selectEditor(project, fileEditor, databaseFile, editorProviderId, instructions);
                sourceCodeEditor.navigateTo(object);
                break;
            }
        }
    }

    private static void invokeFileOpen(DBFileOpenHandle handle, Runnable opener) {
        if (ProgressMonitor.isProgressCancelled()) {
            handle.release();
        } else {
            DBObjectVirtualFile file = handle.getObject().getVirtualFile();
            try {
                markSkipBrowserAutoscroll(file);
                opener.run();
            } finally {
                unmarkSkipBrowserAutoscroll(file);
                handle.release();
            }
        }
    }

    private static void prepareEditor(@NotNull DBEditableObjectVirtualFile databaseFile, @NotNull Runnable callback) {
        DBSchemaObject object = databaseFile.getObject();
        DBContentType contentType = object.getContentType();
        if (contentType == DBContentType.DATA) {
            prepareDatasetEditor(databaseFile, callback);

        } else if (contentType.isOneOf(DBContentType.CODE, DBContentType.CODE_SPEC_AND_BODY)) {
            prepareSourcecodeEditor(databaseFile, callback);

        } else {
            callback.run();
        }
    }

    private static void prepareSourcecodeEditor(@NotNull DBEditableObjectVirtualFile databaseFile, @NotNull Runnable callback) {
        // do not prompt ddl file attachments during workspace restore or debugger navigation
        ThreadInfo threadInfo = ThreadInfo.current();
        if (threadInfo.isOneOf(WORKSPACE_RESTORE, DEBUGGER_NAVIGATION)) {
            callback.run();
            return;
        }

        DBSchemaObject object = databaseFile.getObject();
        Project project = object.getProject();

        DDLFileGeneralSettings ddlFileSettings = DDLFileSettings.getInstance(project).getGeneralSettings();
        ConnectionHandler connection = object.getConnection();
        boolean ddlFileBinding = connection.getSettings().getDetailSettings().isEnableDdlFileBinding();
        if (!ddlFileBinding || !ddlFileSettings.isDdlFilesLookupEnabled()) {
            callback.run();
            return;
        }

        List<VirtualFile> attachedDDLFiles = databaseFile.getAttachedDDLFiles();
        if (attachedDDLFiles != null && !attachedDDLFiles.isEmpty()) {
            callback.run();
            return;
        }

        DDLFileAttachmentManager fileAttachmentManager = DDLFileAttachmentManager.getInstance(project);
        DBObjectRef<DBSchemaObject> objectRef = DBObjectRef.of(object);
        List<VirtualFile> ddlFiles = fileAttachmentManager.lookupDetachedDDLFiles(objectRef);
        if (!ddlFiles.isEmpty()) {
            List<VirtualFileInfo> fileInfos = VirtualFileInfo.fromFiles(ddlFiles, project);
            fileAttachmentManager.showFileAttachDialog(object, fileInfos, true,
                    (dialog, exitCode) -> when(exitCode != DialogWrapper.CANCEL_EXIT_CODE, callback));

            return;
        }

        if (ddlFileSettings.isDdlFilesCreationEnabled()) {
            Messages.showQuestionDialog(
                    project, "No DDL file found",
                    "Could not find any DDL file for " + object.getQualifiedNameWithType() + ". Do you want to create one? \n" +
                            "(You can disable this check in \"DDL File\" options)", Messages.OPTIONS_YES_CANCEL, 0,
                    option -> {
                        when(option == 0, () -> fileAttachmentManager.createDDLFile(objectRef));
                        when(option == 1, callback);
                    });
            return;
        }

        callback.run();
    }

    private static void prepareDatasetEditor(DBEditableObjectVirtualFile databaseFile, @NotNull Runnable callback) {
        // do not prompt filter dialogs attachments during workspace restore
        if (ThreadInfo.current().is(WORKSPACE_RESTORE)) {
            callback.run();
            return;
        }

        DBDataset dataset = (DBDataset) databaseFile.getObject();
        Project project = dataset.getProject();

        DatasetFilterManager filterManager = DatasetFilterManager.getInstance(project);
        DatasetFilter filter = filterManager.getActiveFilter(dataset);
        if (filter != null) {
            callback.run();
            return;
        }


        DataEditorSettings settings = DataEditorSettings.getInstance(project);
        if (!settings.getFilterSettings().isPromptFilterDialog()) {
            callback.run();
            return;
        }

        DatasetFilterType defaultFilterType = settings.getFilterSettings().getDefaultFilterType();
        filterManager.openFiltersDialog(dataset, true, false, defaultFilterType,
                (dialog, exitCode) -> when(exitCode != DialogWrapper.CANCEL_EXIT_CODE, callback));

    }


    public void openDatabaseConsole(DBConsole console, boolean scrollBrowser, boolean focusEditor) {
        ConnectionHandler connection = console.getConnection();
        Project project = connection.getProject();

        NavigationInstructions editorInstructions = NavigationInstructions.create().with(OPEN).with(SCROLL, focusEditor).with(FOCUS, focusEditor);
        NavigationInstructions browserInstructions = NavigationInstructions.create().with(SCROLL, scrollBrowser);
        DBFileOpenHandle handle = DBFileOpenHandle.create(console).
                withEditorInstructions(editorInstructions).
                withBrowserInstructions(browserInstructions);

        invokeFileOpen(handle, () -> Editors.openFileEditor(project, console.getVirtualFile(), focusEditor));
    }

    public void closeEditor(DBSchemaObject object) {
        VirtualFile file = getFileSystem().findDatabaseFile(object);
        if (file == null) return;

        DatabaseFileManager databaseFileManager = DatabaseFileManager.getInstance(object.getProject());
        databaseFileManager.closeFile(file);
    }

    public void reopenEditor(DBSchemaObject object) {
        Project project = object.getProject();
        VirtualFile file = getFileSystem().findOrCreateDatabaseFile(object);
        if (isNotValid(file)) return;

        Editors.closeFileEditors(project, file);
        Editors.openFileEditor(project, file, false);
    }

    private boolean isEditable(DBObject object) {
        if (isNotValid(object)) return false;
        if (object.isEditable()) return true;

        DBObject parentObject = object.getParentObject();
        if (parentObject instanceof DBSchemaObject && parentObject != object) {
            return isEditable(parentObject);
        }

        return false;
    }


    private static DatabaseFileSystem getFileSystem() {
        return DatabaseFileSystem.getInstance();
    }
}
