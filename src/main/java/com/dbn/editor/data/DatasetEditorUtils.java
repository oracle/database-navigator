/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.editor.data;

import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.object.DBColumn;
import com.dbn.object.DBDataset;
import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.dbn.common.Priority.HIGH;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class DatasetEditorUtils {
    public static List<String> loadDistinctColumnValues(@NotNull DBColumn column) {
        try {
            return DatabaseInterfaceInvoker.load(HIGH,
                    "Loading data",
                    "Loading possible values for " + column.getQualifiedNameWithType(),
                    column.getProject(),
                    column.getConnectionId(),
                    conn -> loadDistinctColumnValues(column, conn));
        } catch (Exception e) {
            conditionallyLog(e);
            return Collections.emptyList();
        }
    }

    @NotNull
    private static List<String> loadDistinctColumnValues(DBColumn column, DBNConnection conn) throws SQLException {
        List<String> list = new ArrayList<>();
        ResultSet resultSet = null;
        try {
            DBDataset dataset = column.getDataset();
            DatabaseMetadataInterface metadata = column.getMetadataInterface();
            resultSet = metadata.getDistinctValues(
                    dataset.getSchemaName(true),
                    dataset.getName(true),
                    column.getName(true),
                    conn);

            while (resultSet.next()) {
                String value = resultSet.getString(1);
                list.add(value);
            }
        } finally {
            Resources.close(resultSet);
        }
        return list;
    }
}
