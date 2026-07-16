package com.dbn.editor.code.source.impl;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseJavaInterface;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBJavaResource;

import java.sql.ResultSet;
import java.sql.SQLException;

import static com.dbn.object.type.DBObjectType.JAVA_RESOURCE;

public class DBJavaResourceSourceCodeAdapter extends DBMetadataSourceCodeAdapter<DBJavaResource> {
    public DBJavaResourceSourceCodeAdapter() { super(JAVA_RESOURCE); }

    @Override
    public ResultSet loadSourceCode(DBJavaResource javaResource, DBContentType contentType, DBNConnection connection) throws SQLException {
        DatabaseMetadataInterface metadataInterface = javaResource.getMetadataInterface();
        String sourceCode = metadataInterface.loadJavaResourceSourceCode(
                javaResource.getSchemaName(),
                javaResource.getName(),
                connection);
        return createSourceCodeResultSet(sourceCode);
    }

    @Override
    public void saveSourceCode(DBJavaResource javaResource, DBContentType contentType, String oldCode, String newCode, DBNConnection connection) throws SQLException {
        DatabaseJavaInterface javaInterface = javaResource.getJavaInterface();
        javaInterface.updateJavaResource(
                javaResource.getSchemaName(true),
                javaResource.getName(true),
                newCode.getBytes(),
                connection);
    }
}
