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

package com.dbn.editor.json.schema;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.listener.DBNFileEditorManagerListener;
import com.dbn.common.util.Unsafe;
import com.dbn.editor.json.JsonFileCache;
import com.dbn.object.DBJsonView;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.jetbrains.jsonSchema.impl.JsonSchemaObject;
import com.jetbrains.jsonSchema.impl.JsonSchemaReader;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.options.setting.Settings.newStateElement;
import static com.jetbrains.jsonSchema.impl.JsonCachedValues.OBJECT_FOR_FILE_KEY;

@State(
    name = JsonDataSchemaManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
@Getter
@Setter
public class JsonDataSchemaManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.JsonDataSchemaManager";

    private JsonDataSchemaManager(Project project) {
        super(project, COMPONENT_NAME);

        ProjectEvents.subscribe(project, this, FileEditorManagerListener.FILE_EDITOR_MANAGER, fileEditorManagerListener());
    }

    private static void cacheJsonSchema(DBJsonView jsonView) {
        PsiFile contentPsiFile = JsonFileCache.getJsonContentPsiFile(jsonView);
        CachedValuesManager.getCachedValue(contentPsiFile, OBJECT_FOR_FILE_KEY, createValueProvider(jsonView));
    }

    private static @NotNull CachedValueProvider<JsonSchemaObject> createValueProvider(DBJsonView jsonView) {
        return () -> {
            VirtualFile schemaFile = JsonFileCache.getJsonSchemaFile(jsonView);
            JsonSchemaObject schemaObject = Unsafe.logged(null, () -> JsonSchemaReader.readFromFile(jsonView.getProject(), schemaFile));
            return schemaObject == null ? null : CachedValueProvider.Result.create(schemaObject, ModificationTracker.NEVER_CHANGED);
        };
    }

    public static JsonDataSchemaManager getInstance(@NotNull Project project) {
        return projectService(project, JsonDataSchemaManager.class);
    }

    @NotNull
    private static FileEditorManagerListener fileEditorManagerListener() {
        return new DBNFileEditorManagerListener() {
            @Override
            public void whenFileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                if (!(file instanceof DBEditableObjectVirtualFile)) return;

                DBEditableObjectVirtualFile editableObjectFile = (DBEditableObjectVirtualFile) file;
                DBSchemaObject object = editableObjectFile.getObject();
                if (object instanceof DBJsonView) {
                    DBJsonView jsonView = (DBJsonView) object;
                    cacheJsonSchema(jsonView);
                }
            }
        };
    }

    /****************************************
     *       PersistentStateComponent       *
     *****************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        Element element = newStateElement();
        return element;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {
    }

}
