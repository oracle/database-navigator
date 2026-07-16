package com.dbn.editor.code.source.impl;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBProcedure;

import java.sql.ResultSet;
import java.sql.SQLException;

import static com.dbn.object.type.DBObjectType.PROCEDURE;

public class DBProcedureSourceCodeAdapter extends DBMetadataSourceCodeAdapter<DBProcedure> {
    public DBProcedureSourceCodeAdapter() {
        super(PROCEDURE);
    }

    @Override
    public ResultSet loadSourceCode(DBProcedure procedure, DBContentType contentType, DBNConnection connection) throws SQLException {
        DatabaseMetadataInterface metadataInterface = procedure.getMetadataInterface();
        return metadataInterface.loadObjectSourceCode(
                procedure.getSchemaName(),
                procedure.getName(),
                "PROCEDURE",
                procedure.getOverload(),
                connection);
    }
}
