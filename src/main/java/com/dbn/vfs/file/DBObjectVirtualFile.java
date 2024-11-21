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

import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.common.compatibility.Compatibility;
import com.dbn.common.compatibility.Workaround;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.ref.WeakRefCache;
import com.dbn.common.util.SlowOps;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.dbn.vfs.DBVirtualFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.UnknownFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.io.File;

import static com.dbn.common.dispose.Failsafe.guarded;
import static com.dbn.common.dispose.Failsafe.nd;

public class DBObjectVirtualFile<T extends DBObject> extends DBVirtualFileBase {
    private static final WeakRefCache<DBObjectRef, DBObjectVirtualFile> virtualFileCache = WeakRefCache.weakKey();
    protected final DBObjectRef<T> object;

    public DBObjectVirtualFile(@NotNull Project project, @NotNull DBObjectRef<T> object) {
        super(project, object.getFileName());
        this.object = object;
    }

    public static DBObjectVirtualFile<?> of(DBObject object) {
        return of(object.ref());
    }

    public static DBObjectVirtualFile<?> of(DBObjectRef objectRef) {
        return virtualFileCache.get(objectRef, o -> new DBObjectVirtualFile(o.getProject(), o));
    }

    public DBObjectType getObjectType() {
        return object.getObjectType();
    }

    @NotNull
    public DBObjectRef<T> getObjectRef() {
        return object;
    }

    @NotNull
    public T getObject() {
        return DBObjectRef.ensure(object);
    }

    @NotNull
    @Override
    public final ConnectionId getConnectionId() {
        return object.getConnectionId();
    }

    @Override
    @NotNull
    public ConnectionHandler getConnection() {
        ConnectionHandler connection = object.getConnection();
        if (connection == null) {
            connection = ConnectionHandler.get(getConnectionId());
        }
        return nd(connection);
    }

    @Nullable
    @Override
    public SchemaId getSchemaId() {
        return SchemaId.from(getObject().getSchema());
    }

    @Override
    public DatabaseSession getSession() {
        return getConnection().getSessionBundle().getPoolSession();
    }

    @Override
    public boolean isValid() {
        return SlowOps.isValid(object);
    }

    @NotNull
    @Override
    public String getPresentablePath() {
        String connectionName = getConnectionName();

        return connectionName + File.separatorChar +
            getObjectRef().getObjectType().getListName() + File.separatorChar +
            getObjectRef().getQualifiedName();
    }

    @Override
    public @NotNull String getPresentableName() {
        String presentableName = super.getPresentableName();
        if (getObjectType() == DBObjectType.JAVA_CLASS) {
            presentableName = presentableName.replace("/", ".");
        }
        return presentableName;
    }

    private String getConnectionName() {
        return Failsafe.guarded("DISPOSED", this, o -> o.getConnection().getName());
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
    @Workaround
    @Compatibility
    public VirtualFile getParent() {
        return guarded(null, this, f -> f.findParent());
    }

    @Nullable
    private VirtualFile findParent() {
        // TODO review / cleanup
/*
        if (!Traces.isCalledThrough(
                "com.intellij.ide.navigationToolbar.NavBarPresentation",
                "com.intellij.ide.navbar.ide.NavBarServiceKt")) return null;
*/

        T object = this.object.get();
        if (object == null) return null;

        BrowserTreeNode treeParent = object.getParent();
        if (treeParent == null) return null;

        if (treeParent instanceof DBObjectList<?>) {
            DBObjectList objectList = (DBObjectList) treeParent;
            PsiDirectory psiDirectory = objectList.getPsiDirectory();
            return psiDirectory.getVirtualFile();
        }
        return null;
    }


    @Override
    public Icon getIcon() {
        T object = guarded(null, () -> getObject());
        return object == null ? getObjectType().getIcon() : object.getIcon();
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

