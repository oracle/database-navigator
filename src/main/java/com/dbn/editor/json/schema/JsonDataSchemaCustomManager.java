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
import com.dbn.common.Reflection;
import com.dbn.common.compatibility.Workaround;
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
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.options.setting.Settings.newStateElement;

/**
 * Reflection-based version of {@link JsonDataSchemaManager}
 * (after internalization of cache components)
 */
@State(
    name = JsonDataSchemaCustomManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
@Getter
@Setter
@Workaround
public class JsonDataSchemaCustomManager extends ProjectComponentBase implements PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.JsonDataSchemaManager";

    private JsonDataSchemaCustomManager(Project project) {
        super(project, COMPONENT_NAME);

        ProjectEvents.subscribe(project, this, FileEditorManagerListener.FILE_EDITOR_MANAGER, fileEditorManagerListener());
    }

    private static void cacheJsonSchema(DBJsonView jsonView) {
        PsiFile contentPsiFile = JsonFileCache.getJsonContentPsiFile(jsonView);

        Key<CachedValue<Object>> cachedValueKey = getSchemaCacheKey();
        if (cachedValueKey == null) return;
        CachedValuesManager.getCachedValue(contentPsiFile, cachedValueKey, createValueProvider(jsonView));
    }

    private static @NotNull CachedValueProvider<Object> createValueProvider(DBJsonView jsonView) {
        return () -> {
            VirtualFile schemaFile = JsonFileCache.getJsonSchemaFile(jsonView);
            Object schemaObject = Unsafe.logged(null, () -> readFromFile(jsonView, schemaFile));
            return schemaObject == null ? null : CachedValueProvider.Result.create(schemaObject, ModificationTracker.NEVER_CHANGED);
        };
    }

    @Nullable
    private static Key<CachedValue<Object>> getSchemaCacheKey() {
        //com.jetbrains.jsonSchema.impl.JsonCachedValues.OBJECT_FOR_FILE_KEY
        String schemaCacheClass = "com.jetbrains.jsonSchema.impl.JsonCachedValues";
        return Unsafe.logged(null, () -> Reflection.getFieldValue(schemaCacheClass, "OBJECT_FOR_FILE_KEY"));
    }

    @Nullable
    private static Object readFromFile(DBJsonView jsonView, VirtualFile schemaFile) {
        //return Unsafe.logged(null, () -> JsonSchemaReader.readFromFile(jsonView.getProject(), schemaFile));

        String schemaReaderClass = "com.jetbrains.jsonSchema.impl.JsonSchemaReader";
        return Unsafe.logged(null, () -> Reflection.invokeMethod(
                schemaReaderClass,
                "readFromFile",
                jsonView.getProject(),
                schemaFile));
    }

    public static JsonDataSchemaCustomManager getInstance(@NotNull Project project) {
        return projectService(project, JsonDataSchemaCustomManager.class);
    }

    @NotNull
    private static FileEditorManagerListener fileEditorManagerListener() {
        return new DBNFileEditorManagerListener() {
            @Override
            public void whenFileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                if (!(file instanceof DBEditableObjectVirtualFile editableObjectFile)) return;

                DBSchemaObject object = editableObjectFile.getObject();
                if (object instanceof DBJsonView jsonView) {
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
