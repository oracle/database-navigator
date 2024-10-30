package com.dbn.object.common;

import com.dbn.api.database.DatabaseMetadata;
import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.common.dispose.StatefulDisposable;
import com.dbn.common.routine.Consumer;
import com.dbn.connection.SchemaId;
import com.dbn.data.type.DBDataTypeBundle;
import com.dbn.data.type.DBNativeDataType;
import com.dbn.database.DatabaseObjectIdentifier;
import com.dbn.object.*;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.common.list.DBObjectListContainer;
import com.dbn.object.type.DBObjectType;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface DBObjectBundle extends BrowserTreeNode, StatefulDisposable, DatabaseMetadata {
    List<DBConsole> getConsoles();

    List<DBSchema> getSchemas(boolean filtered);

    List<DBSchema> getSchemas();

    List<DBSchema> getPublicSchemas();

    List<SchemaId> getSchemaIds();

    @Nullable
    List<DBUser> getUsers();

    @Nullable
    List<DBRole> getRoles();

    @Nullable
    List<DBSystemPrivilege> getSystemPrivileges();

    @Nullable
    List<DBCharset> getCharsets();

    @Nullable
    DBNativeDataType getNativeDataType(String name);

    @Nullable
    DBSchema getSchema(String name);

    @Nullable
    DBSchema getPublicSchema();

    @Nullable
    DBSchema getUserSchema();

    @Nullable
    DBUser getUser(String name);

    @Nullable
    DBRole getRole(String name);

    @Nullable
    DBPrivilege getPrivilege(String name);

    @Nullable
    DBSystemPrivilege getSystemPrivilege(String name);

    @Nullable
    DBCharset getCharset(String name);

    @NotNull
    DBDataTypeBundle getDataTypes();

    @Nullable
    DBObject getObject(DatabaseObjectIdentifier objectIdentifier);

    @Nullable
    DBObject getObject(DBObjectType objectType, String name);

    @Nullable
    DBObject getObject(DBObjectType objectType, String name, short overload);

    void lookupObjectsOfType(Consumer<? super DBObject> consumer, DBObjectType objectType);

    void lookupChildObjectsOfType(Consumer<? super DBObject> consumer, DBObject parentObject, DBObjectType objectType, ObjectTypeFilter filter, DBSchema currentSchema);

    DBObjectListContainer getObjectLists();

    DBObjectInitializer getObjectInitializer();

    <T extends DBObject> DBObjectList<T> getObjectList(DBObjectType objectType);

    PsiFile getFakeObjectFile();

    boolean isValid();
}
