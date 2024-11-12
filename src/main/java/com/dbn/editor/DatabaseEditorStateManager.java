package com.dbn.editor;

import com.dbn.DatabaseNavigator;
import com.dbn.browser.options.DatabaseBrowserEditorSettings;
import com.dbn.browser.options.DatabaseBrowserSettings;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.environment.EnvironmentManager;
import com.dbn.common.environment.options.listener.EnvironmentManagerListener;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.listener.DBNFileEditorManagerListener;
import com.dbn.common.util.Editors;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConsoleChangeListener;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.dbn.editor.code.SourceCodeEditor;
import com.dbn.editor.code.SourceCodeManagerListener;
import com.dbn.editor.data.DatasetEditor;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.editor.DefaultEditorOption;
import com.dbn.object.type.DBObjectType;
import com.dbn.options.ProjectSettings;
import com.dbn.vfs.file.DBContentVirtualFile;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.val;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.options.setting.Settings.*;
import static com.dbn.editor.DatabaseEditorStateManager.COMPONENT_NAME;

@State(
    name = COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DatabaseEditorStateManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseEditorStateManager";

    private final Map<DBObjectType, EditorProviderId> lastUsedEditorProviders = new ConcurrentHashMap<>();

    private DatabaseEditorStateManager(Project project) {
        super(project, COMPONENT_NAME);

        ProjectEvents.subscribe(project, this, SourceCodeManagerListener.TOPIC, sourceCodeManagerListener());
        ProjectEvents.subscribe(project, this, FileEditorManagerListener.FILE_EDITOR_MANAGER, fileEditorManagerListener());
        ProjectEvents.subscribe(project, this, EnvironmentManagerListener.TOPIC, environmentManagerListener());
    }

    public static DatabaseEditorStateManager getInstance(@NotNull Project project) {
        return projectService(project, DatabaseEditorStateManager.class);
    }

    @NotNull
    private SourceCodeManagerListener sourceCodeManagerListener() {
        return new SourceCodeManagerListener() {
            @Override
            public void sourceCodeLoaded(@NotNull final DBSourceCodeVirtualFile sourceCodeFile, boolean initialLoad) {
                Project project = getProject();
                EnvironmentManager environmentManager = EnvironmentManager.getInstance(project);
                boolean readonly = environmentManager.isReadonly(sourceCodeFile);
                Editors.setEditorsReadonly(sourceCodeFile, readonly);
            }
        };
    }

    @NotNull
    private FileEditorManagerListener fileEditorManagerListener() {
        return new DBNFileEditorManagerListener() {
            @Override
            public void whenSelectionChanged(@NotNull FileEditorManagerEvent event) {
                DBObject oldObject = null;
                DBObject newObject = null;
                EditorProviderId editorProviderId = null;


                FileEditor oldEditor = event.getOldEditor();
                if (oldEditor instanceof SourceCodeEditor) {
                    SourceCodeEditor sourceCodeEditor = (SourceCodeEditor) oldEditor;
                    oldObject = sourceCodeEditor.getObject();
                } else if (oldEditor instanceof DatasetEditor) {
                    DatasetEditor datasetEditor = (DatasetEditor) oldEditor;
                    oldObject = datasetEditor.getDataset();
                }

                FileEditor newEditor = event.getNewEditor();
                if(newEditor != null){
                FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(getProject());
                ConnectionHandler connectionHandler = contextManager.getConnection(newEditor.getFile());
                if(connectionHandler != null) {
                    ProjectEvents.notify(ensureProject(), ConsoleChangeListener.TOPIC, (listener) -> {
                        listener.consoleChanged(connectionHandler.getConnectionId());
                    });
                }
                }
                if (newEditor instanceof SourceCodeEditor) {
                    SourceCodeEditor sourceCodeEditor = (SourceCodeEditor) newEditor;
                    editorProviderId = sourceCodeEditor.getEditorProviderId();
                    newObject = sourceCodeEditor.getObject();
                } else if (newEditor instanceof DatasetEditor) {
                    DatasetEditor datasetEditor = (DatasetEditor) newEditor;
                    newObject = datasetEditor.getDataset();
                    editorProviderId = EditorProviderId.DATA;
                }

                if (editorProviderId != null && oldObject != null && newObject != null && newObject.equals(oldObject)) {
                    DBObjectType objectType = newObject.getObjectType();
                    lastUsedEditorProviders.put(objectType, editorProviderId);
                }
            }
        };
    }

    @NotNull
    private EnvironmentManagerListener environmentManagerListener() {
        return new EnvironmentManagerListener() {
            @Override
            public void configurationChanged(Project project) {
                FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
                EnvironmentManager environmentManager = EnvironmentManager.getInstance(project);
                VirtualFile[] openFiles = fileEditorManager.getOpenFiles();
                for (VirtualFile virtualFile : openFiles) {
                    if (virtualFile instanceof DBEditableObjectVirtualFile) {
                        DBEditableObjectVirtualFile editableDatabaseFile = (DBEditableObjectVirtualFile) virtualFile;
                        if (editableDatabaseFile.isContentLoaded()) {
                            List<DBContentVirtualFile> contentFiles = editableDatabaseFile.getContentFiles();
                            for (DBContentVirtualFile contentFile : contentFiles) {
                                boolean readonly = environmentManager.isReadonly(contentFile);
                                Editors.setEditorsReadonly(contentFile, readonly);
                            }
                        }
                    }
                }
            }
        };
    }

    @Nullable
    public EditorProviderId getEditorProvider(DBObjectType objectType) {
        DatabaseBrowserSettings browserSettings = ProjectSettings.get(getProject()).getBrowserSettings();
        DatabaseBrowserEditorSettings editorSettings = browserSettings.getEditorSettings();
        DefaultEditorOption option = editorSettings.getOption(objectType);
        if (option != null) {
            switch (option.getEditorType()) {
                case SPEC: return EditorProviderId.CODE_SPEC;
                case BODY: return EditorProviderId.CODE_BODY;
                case CODE: return EditorProviderId.CODE;
                case DATA: return EditorProviderId.DATA;
                case SELECTION: return lastUsedEditorProviders.get(objectType);
            }
        }

        return null;
    }

    /****************************************
     *       PersistentStateComponent       *
     *****************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        Element element = new Element("state");
        Element editorProvidersElement = newElement(element, "last-used-providers");
        for (val entry : lastUsedEditorProviders.entrySet()) {
            DBObjectType objectType = entry.getKey();
            EditorProviderId editorProviderId = entry.getValue();

            Element objectTypeElement = newElement(editorProvidersElement, "object-type");
            setEnumAttribute(objectTypeElement, "object-type", objectType);
            setEnumAttribute(objectTypeElement, "editor-provider", editorProviderId);

        }
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
        lastUsedEditorProviders.clear();
        Element editorProvidersElement = element.getChild("last-used-providers");
        if (editorProvidersElement != null) {
            for (Element child : editorProvidersElement.getChildren()) {
                DBObjectType objectType = DBObjectType.get(stringAttribute(child, "object-type"));
                EditorProviderId editorProviderId = enumAttribute(child, "editor-provider", EditorProviderId.class);
                lastUsedEditorProviders.put(objectType, editorProviderId);
            }
        }
/*
        recordViewColumnSortingType = SettingsUtil.getEnum(element, "record-view-column-sorting-type", recordViewColumnSortingType);
        valuePreviewTextWrapping = SettingsUtil.getBoolean(element, "value-preview-text-wrapping", valuePreviewTextWrapping);
        valuePreviewTextWrapping = SettingsUtil.getBoolean(element, "value-preview-pinned", valuePreviewPinned);
*/
    }

}
