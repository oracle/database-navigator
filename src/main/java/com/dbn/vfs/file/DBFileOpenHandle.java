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

package com.dbn.vfs.file;

import com.dbn.common.navigation.NavigationInstructions;
import com.dbn.common.util.Unsafe;
import com.dbn.editor.DatabaseEditorStateManager;
import com.dbn.editor.EditorProviderId;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public final class DBFileOpenHandle {
    private static final Set<DBObjectRef<?>> REGISTRY = ConcurrentHashMap.newKeySet();

    private final DBObjectRef<?> object;
    private EditorProviderId editorProviderId;
    private NavigationInstructions browserInstructions = NavigationInstructions.NONE;
    private NavigationInstructions editorInstructions = NavigationInstructions.NONE;

    private DBFileOpenHandle(@NotNull DBObject object) {
        this.object = DBObjectRef.of(object);
    }

    public static <T extends DBObject> DBFileOpenHandle create(@NotNull T object) {
        return new DBFileOpenHandle(object);
    }

    public DBFileOpenHandle withBrowserInstructions(@NotNull NavigationInstructions browserInstructions) {
        this.browserInstructions = browserInstructions;
        return this;
    }

    public DBFileOpenHandle withEditorInstructions(@NotNull NavigationInstructions editorInstructions) {
        this.editorInstructions = editorInstructions;
        return this;
    }

    public DBFileOpenHandle withEditorProviderId(EditorProviderId editorProviderId) {
        this.editorProviderId = editorProviderId;
        return this;
    }

    public EditorProviderId getEditorProviderId() {
        if (editorProviderId == null) {
            DatabaseEditorStateManager editorStateManager = DatabaseEditorStateManager.getInstance(getObject().getProject());
            editorProviderId = editorStateManager.getEditorProvider(object.getObjectType());
        }
        return editorProviderId;
    }

    @NotNull
    public <T extends DBObject> T getObject() {
        return Unsafe.cast(DBObjectRef.ensure(object));
    }

    public static boolean isFileOpening(@NotNull DBObject object) {
        return REGISTRY.contains(object.ref());
    }

    public void init() {
        REGISTRY.add(object);
    }

    public void release() {
        REGISTRY.remove(object);
    }
}
