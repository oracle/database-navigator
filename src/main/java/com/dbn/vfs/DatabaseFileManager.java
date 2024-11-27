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

package com.dbn.vfs;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.Components;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.component.ProjectManagerListener;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.exception.ProcessDeferredException;
import com.dbn.common.listener.DBNFileEditorManagerListener;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.thread.ThreadMonitor;
import com.dbn.common.thread.ThreadProperty;
import com.dbn.connection.ConnectionAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.ConnectionConfigListener;
import com.dbn.editor.DatabaseFileEditorManager;
import com.dbn.editor.code.SourceCodeManager;
import com.dbn.editor.code.diff.SourceCodeDiffManager;
import com.dbn.editor.code.options.CodeEditorChangesOption;
import com.dbn.editor.code.options.CodeEditorConfirmationSettings;
import com.dbn.editor.code.options.CodeEditorSettings;
import com.dbn.object.DBConsole;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.dbn.vfs.file.DBObjectVirtualFile;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.util.Commons.list;
import static com.dbn.common.util.Lists.anyMatch;

@State(
    name = DatabaseFileManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
@Getter
@Setter
public class DatabaseFileManager extends ProjectComponentBase implements PersistentState, ProjectManagerListener {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseFileManager";

    private final Set<DBObjectVirtualFile<?>> openFiles = ContainerUtil.newConcurrentSet();
    private Map<ConnectionId, List<DBObjectRef<DBSchemaObject>>> pendingOpenFiles = new HashMap<>();
    private final String sessionId;

    private DatabaseFileManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
        sessionId = UUID.randomUUID().toString();

        ProjectEvents.subscribe(project, this, FileEditorManagerListener.FILE_EDITOR_MANAGER, fileEditorManagerListener());
        ProjectEvents.subscribe(project, this, FileEditorManagerListener.Before.FILE_EDITOR_MANAGER, fileEditorManagerListenerBefore());
        ProjectEvents.subscribe(project, this,
                ConnectionConfigListener.TOPIC,
                ConnectionConfigListener.whenChangedOrRemoved(id -> closeFiles(id)));

    }

    public static DatabaseFileManager getInstance(@NotNull Project project) {
        return Components.projectService(project, DatabaseFileManager.class);
    }

    @NotNull
    private FileEditorManagerListener fileEditorManagerListener() {
        return new DBNFileEditorManagerListener() {
            @Override
            public void whenFileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                if (file instanceof DBObjectVirtualFile) {
                    DBObjectVirtualFile<?> databaseFile = (DBObjectVirtualFile<?>) file;
                    openFiles.add(databaseFile);
                }
            }

            @Override
            public void whenFileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                if (file instanceof DBObjectVirtualFile) {
                    DBObjectVirtualFile<?> databaseFile = (DBObjectVirtualFile<?>) file;
                    openFiles.remove(databaseFile);
                }
            }

            @Override
            public void whenSelectionChanged(@NotNull FileEditorManagerEvent event) {

            }
        };
    }

    @NotNull
    private FileEditorManagerListener.Before fileEditorManagerListenerBefore() {
        return new FileEditorManagerListener.Before() {
            @Override
            public void beforeFileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                if (file instanceof DBEditableObjectVirtualFile) {
                    DBEditableObjectVirtualFile databaseFile = (DBEditableObjectVirtualFile) file;
                    DBObjectRef<DBSchemaObject> objectRef = databaseFile.getObjectRef();
                    objectRef.ensure();
                }
            }

            @Override
            public void beforeFileClosed(@NotNull FileEditorManager editorManager, @NotNull VirtualFile file) {
                if (!(file instanceof DBEditableObjectVirtualFile)) return;

                DBEditableObjectVirtualFile databaseFile = (DBEditableObjectVirtualFile) file;
                if (!databaseFile.isModified()) return;

                DBSchemaObject object = databaseFile.getObject();
                String objectDescription = object.getQualifiedNameWithType();
                Project project = getProject();

                CodeEditorConfirmationSettings confirmationSettings = CodeEditorSettings.getInstance(project).getConfirmationSettings();
                confirmationSettings.getExitOnChanges().resolve(
                        list(objectDescription),
                        option -> processCodeChangeOption(databaseFile, option));
                // TODO fix - this prevents the other files from being closed in a "close all.." bulk action
                throw new ProcessDeferredException();

            }
        };
    }

    private void processCodeChangeOption(DBEditableObjectVirtualFile file, CodeEditorChangesOption option) {
        Project project = getProject();
        SourceCodeManager sourceCodeManager = SourceCodeManager.getInstance(project);
        switch (option) {
            case CANCEL: break;
            case SAVE: sourceCodeManager.saveSourceCodeChanges(file, () -> closeFile(file)); break;
            case DISCARD: sourceCodeManager.revertSourceCodeChanges(file, () -> closeFile(file)); break;
            case SHOW: {
                List<DBSourceCodeVirtualFile> sourceCodeFiles = file.getSourceCodeFiles();
                for (DBSourceCodeVirtualFile sourceCodeFile : sourceCodeFiles) {
                    if (sourceCodeFile.isModified()) {
                        SourceCodeDiffManager diffManager = SourceCodeDiffManager.getInstance(project);
                        diffManager.opedDatabaseDiffWindow(sourceCodeFile);
                    }
                }
            }
        }
    }

    public boolean isFileOpened(@NotNull DBObject object) {
        return anyMatch(openFiles, file -> file.getObjectRef().is(object));
    }

    public boolean isFileOpened(@NotNull DBObjectRef object) {
        return anyMatch(openFiles, file -> Objects.equals(file.getObjectRef(), object));
    }

    private void closeFiles(ConnectionId connectionId) {
        for (DBObjectVirtualFile<?> file : openFiles) {
            if (file.getConnectionId() == connectionId) {
                closeFile(file);
            }
        }
    }

    public void closeFile(DBSchemaObject object) {
        if (isFileOpened(object)) {
            closeFile(object.getVirtualFile());
        }
    }

    public void closeFile(@NotNull VirtualFile file) {
        FileEditorManager editorManager = FileEditorManager.getInstance(getProject());
        Dispatch.run(true, () -> editorManager.closeFile(file));
    }

    public void closeDatabaseFiles(@NotNull final List<ConnectionId> connectionIds) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(getProject());
        for (VirtualFile virtualFile : fileEditorManager.getOpenFiles()) {
            if (virtualFile instanceof DBVirtualFileBase) {
                DBVirtualFileBase databaseVirtualFile = (DBVirtualFileBase) virtualFile;
                ConnectionId connectionId = databaseVirtualFile.getConnectionId();
                if (connectionIds.contains(connectionId)) {
                    closeFile(virtualFile);
                }
            }
        }
    }

    /*********************************************
     *            PersistentStateComponent       *
     *********************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        Element element = newElement("state");
        Element filesElement = newElement(element, "open-files");
        for (DBObjectVirtualFile<?> openFile : openFiles) {
            DBObjectRef<?> objectRef = openFile.getObjectRef();
            Element fileElement = newElement(filesElement, "object");
            objectRef.writeState(fileElement);
        }

        return element;
    }

    @Override
    public void loadComponentState(@NotNull @NonNls Element element) {
        Element openFilesElement = element.getChild("open-files");
        if (openFilesElement == null || pendingOpenFiles == null) return;

        List<Element> fileElements = openFilesElement.getChildren();
        for (Element fileElement : fileElements) {
            DBObjectRef<DBSchemaObject> objectRef = DBObjectRef.from(fileElement);
            if (objectRef == null) continue;

            ConnectionId connectionId = objectRef.getConnectionId();
            var objectRefs = pendingOpenFiles.computeIfAbsent(connectionId, id -> new ArrayList<>());
            objectRefs.add(objectRef);
        }
    }

    public void reopenDatabaseEditors() {
        if (pendingOpenFiles == null || pendingOpenFiles.isEmpty()) return;

        // overwrite and nullify
        var pendingOpenFiles = this.pendingOpenFiles;
        this.pendingOpenFiles = null;

        for (var entry : pendingOpenFiles.entrySet()) {
            ConnectionId connectionId = entry.getKey();
            ConnectionHandler connection = ConnectionHandler.get(connectionId);
            if (connection == null) continue;

            var connectionDetailSettings = connection.getSettings().getDetailSettings();
            if (!connectionDetailSettings.isRestoreWorkspace()) continue;

            reopenDatabaseEditors(entry.getValue(), connection);
        }
    }

    private void reopenDatabaseEditors(@NotNull List<DBObjectRef<DBSchemaObject>> objects, @NotNull ConnectionHandler connection) {
        Project project = connection.getProject();
        ConnectionAction.invoke(txt("app.connection.activity.OpeningDatabaseEditors"), false, connection, action ->
                ThreadMonitor.surround(ThreadProperty.WORKSPACE_RESTORE, () ->
                        Progress.background(project, connection, true,
                                txt("prc.workspace.title.RestoringWorkspace"),
                                txt("prc.workspace.message.RestoringWorkspace", connection.getName()),
                                progress -> reopenDatabaseEditors(objects, connection, progress))));
    }

    private static void reopenDatabaseEditors(@NotNull List<DBObjectRef<DBSchemaObject>> objects, @NotNull ConnectionHandler connection, ProgressIndicator progress) {
        Project project = connection.getProject();
        progress.setIndeterminate(true);
        var editorManager = DatabaseFileEditorManager.getInstance(project);

        for (DBObjectRef<DBSchemaObject> objectRef : objects) {
            if (progress.isCanceled()) continue;
            if (!connection.canConnect()) continue;

            DBObject object = objectRef.get();
            if (object == null) continue;

            progress.setText2(connection.getName() + " - " + objectRef.getQualifiedNameWithType());
            if (object instanceof DBConsole) {
                DBConsole console = (DBConsole) object;
                editorManager.openDatabaseConsole(console, false, false);
            } else {
                editorManager.openEditor(object, null, false, false);
            }
        }
    }
}
