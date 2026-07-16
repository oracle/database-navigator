package com.dbn.editor.code.source.impl;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBFunction;

import java.sql.ResultSet;
import java.sql.SQLException;

import static com.dbn.object.type.DBObjectType.FUNCTION;

public class DBFunctionSourceCodeAdapter extends DBMetadataSourceCodeAdapter<DBFunction> {
    public DBFunctionSourceCodeAdapter() {
        super(FUNCTION);
    }

    @Override
    public ResultSet loadSourceCode(DBFunction function, DBContentType contentType, DBNConnection connection) throws SQLException {
        DatabaseMetadataInterface metadataInterface = function.getMetadataInterface();
        return metadataInterface.loadObjectSourceCode(
                function.getSchemaName(),
                function.getName(),
                "FUNCTION",
                function.getOverload(),
                connection);
    }
}
