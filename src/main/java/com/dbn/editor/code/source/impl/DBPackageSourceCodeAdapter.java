package com.dbn.editor.code.source.impl;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBPackage;

import java.sql.ResultSet;
import java.sql.SQLException;

import static com.dbn.object.type.DBObjectType.PACKAGE;

public class DBPackageSourceCodeAdapter extends DBMetadataSourceCodeAdapter<DBPackage> {
    public DBPackageSourceCodeAdapter() { super(PACKAGE); }

    @Override
    public ResultSet loadSourceCode(DBPackage pckage, DBContentType contentType, DBNConnection connection) throws SQLException {
        String qualifier =
                contentType == DBContentType.CODE_SPEC ? "PACKAGE" :
                contentType == DBContentType.CODE_BODY ? "PACKAGE BODY" : null;

        DatabaseMetadataInterface metadataInterface = pckage.getMetadataInterface();
        return metadataInterface.loadObjectSourceCode(
                pckage.getSchemaName(),
                pckage.getName(),
                qualifier,
                connection);
    }
}
