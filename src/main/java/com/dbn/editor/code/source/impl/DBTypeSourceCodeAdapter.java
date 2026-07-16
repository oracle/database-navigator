package com.dbn.editor.code.source.impl;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBType;

import java.sql.ResultSet;
import java.sql.SQLException;

import static com.dbn.object.type.DBObjectType.TYPE;

public class DBTypeSourceCodeAdapter extends DBMetadataSourceCodeAdapter<DBType> {
    public DBTypeSourceCodeAdapter() {
        super(TYPE);
    }

    @Override
    public ResultSet loadSourceCode(DBType type, DBContentType contentType, DBNConnection connection) throws SQLException {
        String qualifier =
                contentType == DBContentType.CODE_SPEC ? "TYPE" :
                contentType == DBContentType.CODE_BODY ? "TYPE BODY" : null;

        DatabaseMetadataInterface metadataInterface = type.getMetadataInterface();
        return metadataInterface.loadObjectSourceCode(
                type.getSchemaName(),
                type.getName(),
                qualifier,
                connection);
    }
}
