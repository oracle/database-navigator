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
