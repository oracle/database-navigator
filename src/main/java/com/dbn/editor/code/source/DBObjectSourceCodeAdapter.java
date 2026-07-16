package com.dbn.editor.code.source;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.editor.DBContentType;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.extension.DBObjectExtensionPoint;
import com.intellij.openapi.extensions.ExtensionPointName;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface DBObjectSourceCodeAdapter<T extends DBSchemaObject> extends DBObjectExtensionPoint {
    ExtensionPointName<DBObjectSourceCodeAdapter> EP = ExtensionPointName.create("com.dbn.objectSourceCodeAdapter");

    ResultSet loadSourceCode(
            T object,
            DBContentType contentType,
            DBNConnection connection) throws SQLException;

    default ResultSet loadReadonlySourceCode(
            T object,
            DBContentType contentType,
            DBNConnection connection) throws SQLException { return null; }

    void saveSourceCode(
            T object,
            DBContentType contentType,
            String oldCode,
            String newCode,
            DBNConnection connection) throws SQLException;
}
