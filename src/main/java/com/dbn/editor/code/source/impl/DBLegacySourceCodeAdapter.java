package com.dbn.editor.code.source.impl;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.editor.DBContentType;
import com.dbn.editor.code.source.DBObjectSourceCodeAdapter;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.extension.DBObjectExtensionPointBase;
import com.dbn.object.type.DBObjectType;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DBLegacySourceCodeAdapter extends DBObjectExtensionPointBase implements DBObjectSourceCodeAdapter<DBSchemaObject> {
    public DBLegacySourceCodeAdapter() {
        super(DBObjectType.ANY);
    }

    @Override
    public ResultSet loadSourceCode(DBSchemaObject object, DBContentType contentType, DBNConnection connection) {
        return null;
    }

    @Override
    public void saveSourceCode(DBSchemaObject object, DBContentType contentType, String oldCode, String newCode, DBNConnection connection) throws SQLException {
        object.executeUpdateDDL(contentType, oldCode, newCode);
    }
}
