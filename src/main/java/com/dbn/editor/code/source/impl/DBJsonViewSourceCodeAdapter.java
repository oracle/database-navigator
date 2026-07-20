package com.dbn.editor.code.source.impl;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBJsonView;

import java.sql.ResultSet;
import java.sql.SQLException;

import static com.dbn.object.type.DBObjectType.JSON_VIEW;

public class DBJsonViewSourceCodeAdapter extends DBMetadataSourceCodeAdapter<DBJsonView> {
    public DBJsonViewSourceCodeAdapter() { super(JSON_VIEW); }

    @Override
    public ResultSet loadSourceCode(DBJsonView view, DBContentType contentType, DBNConnection connection) throws SQLException {
        DatabaseMetadataInterface metadataInterface = view.getMetadataInterface();
        return metadataInterface.loadViewSourceCode(
                view.getSchemaName(),
                view.getName(),
                connection);
    }

    @Override
    public void saveSourceCode(DBJsonView view, DBContentType contentType, String oldCode, String newCode, DBNConnection connection) throws SQLException {
        DatabaseDataDefinitionInterface dataDefinitionInterface = view.getDataDefinitionInterface();
        dataDefinitionInterface.updateJsonView(
                view.getSchemaName(true),
                view.getName(true),
                newCode,
                view.isEditionable(),
                connection);
    }
}
