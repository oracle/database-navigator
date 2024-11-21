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

import com.dbn.connection.session.DatabaseSession;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBDataset;
import org.jetbrains.annotations.NotNull;

public class DBDatasetVirtualFile extends DBContentVirtualFile {
    DBDatasetVirtualFile(DBEditableObjectVirtualFile databaseFile, DBContentType contentType) {
        super(databaseFile, contentType);
    }

    @Override
    @NotNull
    public DBDataset getObject() {
        return (DBDataset) super.getObject();
    }

    @Override
    public DatabaseSession getSession() {
        return this.getConnection().getSessionBundle().getMainSession();
    }
}
