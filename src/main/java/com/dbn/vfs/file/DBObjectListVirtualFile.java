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

import com.dbn.common.dispose.Checks;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.util.Naming;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.DatabaseEntity;
import com.dbn.connection.SchemaId;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.object.common.DBObjectPsiCache;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.type.DBObjectType;
import com.dbn.vfs.DBVirtualFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.dispose.Failsafe.guarded;

public class DBObjectListVirtualFile<T extends DBObjectList> extends DBVirtualFileBase {
    private final WeakRef<T> objectList;

    public DBObjectListVirtualFile(T objectList) {
        super(objectList.getProject(), Naming.capitalize(objectList.getName()));
        this.objectList = WeakRef.of(objectList);
    }

    @Override
    public DBObjectType getObjectType() {
        T objectList = this.objectList.get();
        return objectList == null ? DBObjectType.UNKNOWN : objectList.getObjectType();
    }

    @NotNull
    public T getObjectList() {
        return objectList.ensure();
    }

    @NotNull
    @Override
    public ConnectionId getConnectionId() {
        return getObjectList().getConnectionId();
    }

    @Override
    @NotNull
    public ConnectionHandler getConnection() {
        return getObjectList().getConnection();
    }

    @Nullable
    @Override
    public SchemaId getSchemaId() {
        DatabaseEntity parent = getObjectList().getParentEntity();
        if (parent instanceof DBObject) {
            DBObject object = (DBObject) parent;
            return SchemaId.from(object.getSchema());
        }
        return null;
    }

    @Nullable
    @Override
    public DatabaseSession getSession() {
        return this.getConnection().getSessionBundle().getPoolSession();
    }

    /*********************************************************
     *                     VirtualFile                       *
     *********************************************************/

    @Override
    @NotNull
    public FileType getFileType() {
        return UnknownFileType.INSTANCE;
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    @Nullable
    public VirtualFile getParent() {
        return guarded(null, this, f -> f.findParent());
    }

    @Nullable
    private VirtualFile findParent() {
        T objectList = this.objectList.get();
        if (isNotValid(objectList)) return null;

        DatabaseEntity parent = getObjectList().getParentEntity();
        if (parent instanceof DBObject) {
            DBObject parentObject = (DBObject) parent;
            return DBObjectPsiCache.asPsiDirectory(parentObject).getVirtualFile();
        }

        if (parent instanceof DBObjectBundle) {
            DBObjectBundle objectBundle = (DBObjectBundle) parent;
            return objectBundle.getConnection().getPsiDirectory().getVirtualFile();
        }

        return null;
    }

    @Override
    public boolean isValid() {
        return Checks.isValid(objectList.get());
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public long getLength() {
        return 0;
    }

    @Override
    public String getExtension() {
        return null;
    }
}

