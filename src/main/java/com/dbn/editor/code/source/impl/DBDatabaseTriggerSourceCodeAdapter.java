package com.dbn.editor.code.source.impl;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBDatabaseTrigger;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.dbn.object.type.DBObjectType.DATABASE_TRIGGER;

public class DBDatabaseTriggerSourceCodeAdapter extends DBMetadataSourceCodeAdapter<DBDatabaseTrigger> {
    public DBDatabaseTriggerSourceCodeAdapter() {
        super(DATABASE_TRIGGER);
    }

    @Override
    public ResultSet loadSourceCode(DBDatabaseTrigger trigger, DBContentType contentType, DBNConnection connection) throws SQLException {
        DatabaseMetadataInterface metadataInterface = trigger.getMetadataInterface();
        return metadataInterface.loadDatabaseTriggerSourceCode(
                trigger.getSchemaName(),
                trigger.getName(),
                connection);
    }
}
