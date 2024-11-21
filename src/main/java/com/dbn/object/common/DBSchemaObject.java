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

package com.dbn.object.common;

import com.dbn.editor.DBContentType;
import com.dbn.language.common.DBLanguage;
import com.dbn.object.DBSchema;
import com.dbn.object.common.status.DBObjectStatusHolder;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.dbn.vfs.file.DBObjectVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;

public interface DBSchemaObject extends DBObject {
    @NotNull
    @Override
    DBSchema getSchema();

    List<DBObject> getReferencedObjects();

    List<DBObject> getReferencingObjects();

    boolean isEditable(DBContentType contentType);

    DBLanguage getCodeLanguage(DBContentType contentType);

    String getCodeParseRootId(DBContentType contentType);

    void executeUpdateDDL(DBContentType contentType, String oldCode, String newCode) throws SQLException;

    DBObjectStatusHolder getStatus();

    @Override
    @NotNull
    DBObjectVirtualFile<?> getVirtualFile();

    DBEditableObjectVirtualFile getEditableVirtualFile();

    @Nullable
    DBEditableObjectVirtualFile getCachedVirtualFile();

    List<DBSchema> getReferencingSchemas() throws SQLException;

    boolean isDisabled();

    default boolean isEnabled() {
        return !isDisabled();
    }
}
