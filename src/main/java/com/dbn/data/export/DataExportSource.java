/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.data.export;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dbn.execution.ExecutionResult;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class DataExportSource {
    private final ResultSetTable<?> table;
    private DBObjectRef<?> object;
    private ExecutionResult result;
    private ConnectionRef connection;

    private DataExportSource(ResultSetTable<?> sourceTable, Object sourceObject) {
        this.table = sourceTable;

        if (sourceObject instanceof DBObject) {
            DBObject object = (DBObject) sourceObject;

            this.object = DBObjectRef.of(object);
            this.connection = object.getConnection().ref();

        } else if (sourceObject instanceof ExecutionResult) {
            ExecutionResult executionResult = (ExecutionResult) sourceObject;

            this.result = executionResult;
            this.connection = executionResult.getConnection().ref();
        }
    }

    public static DataExportSource create(ResultSetTable<?> sourceTable, Object sourceObject) {
        return new DataExportSource(sourceTable, sourceObject);
    }

    @Nullable
    public DBObject getObject() {
        return DBObjectRef.get(object);
    }

    @NotNull
    public ConnectionHandler getConnection() {
        return ConnectionRef.ensure(connection);
    }

    @NotNull
    public Project getProject() {
        return getConnection().getProject();
    }
}
