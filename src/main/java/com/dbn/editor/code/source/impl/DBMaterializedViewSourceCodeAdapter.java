package com.dbn.editor.code.source.impl;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBMaterializedView;

import java.sql.ResultSet;
import java.sql.SQLException;

import static com.dbn.object.type.DBObjectType.MATERIALIZED_VIEW;

public class DBMaterializedViewSourceCodeAdapter extends DBMetadataSourceCodeAdapter<DBMaterializedView> {
    public DBMaterializedViewSourceCodeAdapter() { super(MATERIALIZED_VIEW); }

    @Override
    public ResultSet loadSourceCode(DBMaterializedView view, DBContentType contentType, DBNConnection connection) throws SQLException {
        DatabaseMetadataInterface metadataInterface = view.getMetadataInterface();
        return metadataInterface.loadMaterializedViewSourceCode(
                view.getSchemaName(),
                view.getName(),
                connection);
    }
}
