package com.dbn.object.impl;

import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.def.DBModelMetadata;
import com.dbn.object.DBAIModel;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObjectImpl;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class DBAIModelImpl  extends DBSchemaObjectImpl<DBModelMetadata> implements DBAIModel {
    public DBAIModelImpl(@NotNull DBSchema schema, DBModelMetadata metadata) throws SQLException {
        super(schema, metadata);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBModelMetadata metadata) throws SQLException {
        return metadata.getModelName();
    }

    @Override
    public @NotNull DBObjectType getObjectType() {
        return DBObjectType.AI_MODEL;
    }
}
