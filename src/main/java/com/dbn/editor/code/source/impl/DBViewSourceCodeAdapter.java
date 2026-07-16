package com.dbn.editor.code.source.impl;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBView;

import java.sql.ResultSet;
import java.sql.SQLException;

import static com.dbn.object.type.DBObjectType.VIEW;

public class DBViewSourceCodeAdapter extends DBMetadataSourceCodeAdapter<DBView> {
    public DBViewSourceCodeAdapter() { super(VIEW); }

    @Override
    public ResultSet loadSourceCode(DBView view, DBContentType contentType, DBNConnection connection) throws SQLException {
        DatabaseMetadataInterface metadataInterface = view.getMetadataInterface();
        return metadataInterface.loadViewSourceCode(
                view.getSchemaName(),
                view.getName(),
                connection);
    }

    @Override
    public void saveSourceCode(DBView object, DBContentType contentType, String oldCode, String newCode, DBNConnection connection) throws SQLException {
        DatabaseDataDefinitionInterface dataDefinitionInterface = object.getDataDefinitionInterface();
        dataDefinitionInterface.updateView(
                object.getSchemaName(true),
                object.getName(true),
                newCode,
                object.isEditionable(),
                connection);
    }
}
