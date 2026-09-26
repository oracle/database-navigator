/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.editor.code.source;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.editor.DBContentType;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.extension.DBObjectExtensionPoint;
import com.intellij.openapi.extensions.ExtensionPointName;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface DBObjectSourceCodeAdapter<T extends DBObject> extends DBObjectExtensionPoint {
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
