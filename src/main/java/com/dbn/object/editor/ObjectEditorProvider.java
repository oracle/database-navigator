/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.object.editor;

import com.dbn.object.common.DBObject;
import com.dbn.object.common.extension.DBObjectExtensionPoint;
import com.dbn.object.common.list.DBObjectList;
import com.intellij.openapi.extensions.ExtensionPointName;

/**
 * Extension point that supplies the type-specific create/edit input dialog for a managed {@link DBObject} type.
 * <p>
 * This is the edit-side counterpart of {@link com.dbn.object.factory.ObjectFactoryAdapter}, but works with the
 * connection-centered {@link com.dbn.object.management.ObjectManagementService} rather than the schema-centered
 * object factory. The object lifecycle (create/update/delete) is still executed by the management service; a
 * provider only owns the input UI, keeping generic actions like {@link com.dbn.object.action.ObjectEditAction}
 * and {@link com.dbn.object.common.list.action.ObjectCreateAction} free of any object-type knowledge.
 */
public interface ObjectEditorProvider extends DBObjectExtensionPoint {
    ExtensionPointName<ObjectEditorProvider> EP = ExtensionPointName.create("com.dbn.objectEditorProvider");

    /**
     * Opens the dialog for creating a new object into the given (connection-root) object list.
     */
    void openCreateDialog(DBObjectList objectList);

    /**
     * Opens the dialog for editing the given existing object.
     */
    void openEditDialog(DBObject object);
}
