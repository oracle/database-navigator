package com.dbn.editor.code.source.impl;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.editor.DBContentType;
import com.dbn.editor.code.source.DBObjectSourceCodeAdapter;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.extension.DBObjectExtensionPointBase;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.Nullable;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

abstract class DBMetadataSourceCodeAdapter<T extends DBSchemaObject> extends DBObjectExtensionPointBase implements DBObjectSourceCodeAdapter<T> {
    DBMetadataSourceCodeAdapter(DBObjectType objectType) {
        super(objectType);
    }

    @Override
    public abstract ResultSet loadSourceCode(T object, DBContentType contentType, DBNConnection connection) throws SQLException;

    @Override
    public ResultSet loadReadonlySourceCode(T object, DBContentType contentType, DBNConnection connection) throws SQLException {
        return createSourceCodeResultSet(null);
    }

    protected static ResultSet createSourceCodeResultSet(@Nullable String sourceCode) throws SQLException {
        RowSetMetaDataImpl metadata = new RowSetMetaDataImpl();
        metadata.setColumnCount(1);
        metadata.setColumnName(1, "SOURCE_CODE");
        metadata.setColumnType(1, Types.LONGVARCHAR);

        CachedRowSet resultSet = RowSetProvider.newFactory().createCachedRowSet();
        resultSet.setMetaData(metadata);
        if (sourceCode != null && !sourceCode.isEmpty()) {
            resultSet.moveToInsertRow();
            resultSet.updateString(1, sourceCode);
            resultSet.insertRow();
            resultSet.moveToCurrentRow();
        }
        resultSet.beforeFirst();
        return resultSet;
    }

    @Override
    public void saveSourceCode(T object, DBContentType contentType, String oldCode, String newCode, DBNConnection connection) throws SQLException {
        DatabaseDataDefinitionInterface dataDefinitionInterface = object.getDataDefinitionInterface();
        dataDefinitionInterface.updateObject(
                object.getSchemaName(true),
                object.getName(true),
                object.getObjectType().getName(),
                oldCode,
                newCode,
                connection);
    }
}
