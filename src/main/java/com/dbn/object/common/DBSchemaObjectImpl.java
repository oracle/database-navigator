package com.dbn.object.common;

import com.dbn.common.dispose.Failsafe;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.Resources;
import com.dbn.database.common.metadata.DBObjectMetadata;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.editor.DBContentType;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.psql.PSQLLanguage;
import com.dbn.object.DBSchema;
import com.dbn.object.common.list.DBObjectListContainer;
import com.dbn.object.common.property.DBObjectProperty;
import com.dbn.object.common.status.DBObjectStatus;
import com.dbn.object.common.status.DBObjectStatusHolder;
import com.dbn.object.type.DBObjectType;
import com.dbn.vfs.DatabaseFileSystem;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.dbn.vfs.file.DBObjectVirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.Priority.HIGHEST;
import static com.dbn.common.content.DynamicContentProperty.DEPENDENCY;
import static com.dbn.common.content.DynamicContentProperty.INTERNAL;
import static com.dbn.common.util.Commons.nvln;
import static com.dbn.object.common.property.DBObjectProperty.EDITABLE;
import static com.dbn.object.common.property.DBObjectProperty.REFERENCEABLE;
import static com.dbn.object.common.property.DBObjectProperty.SCHEMA_OBJECT;
import static com.dbn.object.type.DBObjectType.INCOMING_DEPENDENCY;
import static com.dbn.object.type.DBObjectType.OUTGOING_DEPENDENCY;


@Getter
public abstract class DBSchemaObjectImpl<M extends DBObjectMetadata> extends DBObjectImpl<M> implements DBSchemaObject {
    private volatile DBObjectStatusHolder objectStatus;

    public DBSchemaObjectImpl(@NotNull DBSchema schema, M metadata) throws SQLException {
        super(schema, metadata);
    }

    public DBSchemaObjectImpl(@NotNull DBSchemaObject parent, M metadata) throws SQLException {
        super(parent, metadata);
    }

    @Override
    protected void initProperties() {
        properties.set(EDITABLE, true);
        properties.set(REFERENCEABLE, true);
        properties.set(SCHEMA_OBJECT, true);
    }

    @Override
    protected void initLists(ConnectionHandler connection) {
        if (is(REFERENCEABLE)) {
            DBObjectListContainer childObjects = ensureChildObjects();
            childObjects.createObjectList(INCOMING_DEPENDENCY, this, INTERNAL, DEPENDENCY);
            childObjects.createObjectList(OUTGOING_DEPENDENCY, this, INTERNAL, DEPENDENCY);
        }
    }

    @Override
    @NotNull
    public DBSchema getSchema() {
        return Failsafe.nd(super.getSchema());
    }

    @Override
    public DBObjectStatusHolder getStatus() {
        if (objectStatus == null) {
            synchronized (this) {
                if (objectStatus == null) {
                    objectStatus = new DBObjectStatusHolder(getContentType());
                }
            }
        }
        return objectStatus;
    }

    @Override
    public @Nullable Icon getIcon() {
        boolean disabled = isDisabled();
        DBObjectType objectType = getObjectType();
        Icon icon = disabled  ?
                objectType.getDisabledIcon() :
                objectType.getIcon();
        return nvln(icon, objectType.getIcon());
    }

    @Override
    public boolean isDisabled() {
        return is(DBObjectProperty.DISABLEABLE) && !getStatus().is(DBObjectStatus.ENABLED);
    }

    @Override
    public boolean isEditable(DBContentType contentType) {
        return false;
    }

    @Override
    public List<DBObject> getReferencedObjects() {
        return getChildObjects(INCOMING_DEPENDENCY);
    }

    @Override
    public List<DBObject> getReferencingObjects() {
        return getChildObjects(OUTGOING_DEPENDENCY);
    }

    @Override
    public DBLanguage getCodeLanguage(DBContentType contentType) {
        return PSQLLanguage.INSTANCE;
    }

    @Override
    public String getCodeParseRootId(DBContentType contentType) {
        return null;
    }

    @Override
    @NotNull
    public DBObjectVirtualFile<?> getVirtualFile() {
        if (getObjectType().isSchemaObject()) {
            DatabaseFileSystem databaseFileSystem = DatabaseFileSystem.getInstance();
            return databaseFileSystem.findOrCreateDatabaseFile(this);
        }
        return super.getVirtualFile();
    }

    @Override
    public DBEditableObjectVirtualFile getEditableVirtualFile() {
        if (getObjectType().isSchemaObject()) {
            return (DBEditableObjectVirtualFile) getVirtualFile();
        } else {
            return (DBEditableObjectVirtualFile) getParentObject().getVirtualFile();
        }
    }

    @Nullable
    @Override
    public DBEditableObjectVirtualFile getCachedVirtualFile() {
        return DatabaseFileSystem.getInstance().findDatabaseFile(this);
    }

    @Override
    public List<DBSchema> getReferencingSchemas() throws SQLException {
        return loadReferencingSchemas(this);
    }

    private static List<DBSchema> loadReferencingSchemas(DBSchemaObject object) throws SQLException {
        return DatabaseInterfaceInvoker.load(HIGHEST,
                "Loading data dictionary",
                "Loading schema references for " + object.getQualifiedNameWithType(),
                object.getProject(),
                object.getConnectionId(),
                conn -> {
                    List<DBSchema> schemas = new ArrayList<>();
                    ResultSet resultSet = null;
                    try {
                        DBSchema schema = object.getSchema();
                        DatabaseMetadataInterface metadataInterface = object.getMetadataInterface();
                        resultSet = metadataInterface.loadReferencingSchemas(object.getSchemaName(), object.getName(), conn);
                        DBObjectBundle objectBundle = object.getObjectBundle();
                        while (resultSet.next()) {
                            String schemaName = resultSet.getString("SCHEMA_NAME");
                            DBSchema sch = objectBundle.getSchema(schemaName);
                            if (sch != null) {
                                schemas.add(sch);
                            }
                        }
                        if (schemas.isEmpty()) {
                            schemas.add(schema);
                        }

                    } finally {
                        Resources.close(resultSet);
                    }
                    return schemas;
                });
    }

    @Override
    public void executeUpdateDDL(DBContentType contentType, String oldCode, String newCode) throws SQLException {
        DatabaseInterfaceInvoker.execute(HIGHEST,
                "Updating source code",
                "Updating sources of " + getQualifiedNameWithType(),
                getProject(),
                getConnectionId(),
                getSchemaId(),
                conn -> {
                    ConnectionHandler connection = getConnection();
                    DatabaseDataDefinitionInterface dataDefinition = connection.getDataDefinitionInterface();
                    dataDefinition.updateObject(getName(), getObjectType().getName(), oldCode, newCode, conn);
                });
    }

}
