/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.editor.json;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.listener.DBNFileEditorManagerListener;
import com.dbn.editor.DatabaseFileEditorManager;
import com.dbn.editor.EditorProviderId;
import com.dbn.editor.data.DataLoadInstructions;
import com.dbn.editor.data.filter.DatasetFilterInput;
import com.dbn.editor.data.filter.DatasetFilterManager;
import com.dbn.object.DBDataset;
import com.dbn.object.DBJsonView;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.dbn.editor.data.DataLoadInstruction.DELIBERATE_ACTION;
import static com.dbn.editor.data.DataLoadInstruction.PRESERVE_CHANGES;
import static com.dbn.editor.data.DataLoadInstruction.REBUILD;
import static com.dbn.editor.data.DataLoadInstruction.USE_CURRENT_FILTER;

@State(
    name = JsonDataEditorManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
@Getter
@Setter
public class JsonDataEditorManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.JsonDataEditorManager";

    private static final DataLoadInstructions INITIAL_LOAD_INSTRUCTIONS = new DataLoadInstructions(USE_CURRENT_FILTER, PRESERVE_CHANGES, REBUILD);
    private static final DataLoadInstructions RELOAD_LOAD_INSTRUCTIONS = new DataLoadInstructions(USE_CURRENT_FILTER, PRESERVE_CHANGES, DELIBERATE_ACTION);


    private JsonDataEditorManager(Project project) {
        super(project, COMPONENT_NAME);
        ProjectEvents.subscribe(project, this, FileEditorManagerListener.FILE_EDITOR_MANAGER, fileEditorManagerListener());
    }

    public static JsonDataEditorManager getInstance(@NotNull Project project) {
        return projectService(project, JsonDataEditorManager.class);
    }

    @NotNull
    private static FileEditorManagerListener fileEditorManagerListener() {
        return new DBNFileEditorManagerListener() {
            @Override
            public void whenFileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                if (file instanceof DBEditableObjectVirtualFile editableObjectFile) {
                    DBSchemaObject object = editableObjectFile.getObject();
                    if (object instanceof DBJsonView) {
                        FileEditor[] fileEditors = source.getEditors(file);
                        for (FileEditor fileEditor : fileEditors) {
                            if (fileEditor instanceof JsonDataEditor jsonDataEditor) {
                                if (editableObjectFile.getSelectedEditorProviderId() == EditorProviderId.JSON) {
                                    jsonDataEditor.loadData(INITIAL_LOAD_INSTRUCTIONS);
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void whenSelectionChanged(@NotNull FileEditorManagerEvent event) {
                FileEditor newEditor = event.getNewEditor();
                if (newEditor instanceof JsonDataEditor jsonDataEditor) {
                    if (!jsonDataEditor.isLoaded() && !jsonDataEditor.isLoading()) {
                        jsonDataEditor.loadData(INITIAL_LOAD_INSTRUCTIONS);
                    }
                }
            }
        };
    }

    public void reloadEditorData(DBJsonView jsonView) {
        VirtualFile file = jsonView.getVirtualFile();
        FileEditor[] fileEditors = FileEditorManager.getInstance(getProject()).getEditors(file);
        for (FileEditor fileEditor : fileEditors) {
            if (fileEditor instanceof JsonDataEditor datasetEditor) {
                datasetEditor.loadData(RELOAD_LOAD_INSTRUCTIONS);
                break;
            }
        }
    }

    public void openDataEditor(DatasetFilterInput filterInput) {
        DBDataset dataset = filterInput.getDataset();
        Project project = dataset.getProject();

        DatasetFilterManager filterManager = DatasetFilterManager.getInstance(project);
        filterManager.createBasicFilter(filterInput);

        DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(project);
        editorManager.connectAndOpenEditor(dataset, EditorProviderId.DATA, false, true);
    }

    /****************************************
     *       PersistentStateComponent       *
     *****************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        Element element = newStateElement();
/*
        Settings.setEnum(element, "record-view-column-sorting-type", recordViewColumnSortingType);
        Settings.setBoolean(element, "value-preview-text-wrapping", valuePreviewTextWrapping);
        Settings.setBoolean(element, "value-preview-pinned", valuePreviewPinned);
*/
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
/*
        recordViewColumnSortingType = Settings.getEnum(element, "record-view-column-sorting-type", recordViewColumnSortingType);
        valuePreviewTextWrapping = Settings.getBoolean(element, "value-preview-text-wrapping", valuePreviewTextWrapping);
        valuePreviewTextWrapping = Settings.getBoolean(element, "value-preview-pinned", valuePreviewPinned);
*/
    }

}
