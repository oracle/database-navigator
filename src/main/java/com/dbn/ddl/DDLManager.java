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

package com.dbn.ddl;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.PooledConnection;
import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNCallableStatement;
import com.dbn.object.common.DBObject;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.sql.Types;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.util.Strings.cachedUpperCase;

@State(
        name = DDLManager.COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE)
)
public class DDLManager extends ProjectComponentBase implements PersistentState {

    public static final String COMPONENT_NAME = "DBNavigator.Project.DDLManager";

    private DDLManager(@NotNull Project project) {
        super(project, COMPONENT_NAME);
    }

    public static DDLManager getInstance(@NotNull Project project) {
        return projectService(project, DDLManager.class);
    }

    public String extractDDL(DBObject object) throws SQLException {
        // TODO move to database interface (ORACLE)
        ConnectionHandler connection = object.getConnection();
        return PooledConnection.call(connection.createConnectionContext(), conn -> {
            DBNCallableStatement statement = null;
            try {
                DBObjectType objectType = object.getObjectType();
                DBObjectType genericType = objectType.getGenericType();
                objectType = genericType == DBObjectType.TRIGGER ? genericType : objectType;
                String objectTypeName = cachedUpperCase(objectType.getName());

                statement = conn.prepareCall("{? = call DBMS_METADATA.GET_DDL(?, ?, ?)}");
                statement.registerOutParameter(1, Types.CLOB);
                statement.setString(2, objectTypeName);
                statement.setString(3, object.getName());
                statement.setString(4, object.getSchema().getName());

                statement.execute();
                String ddl = statement.getString(1);
                return ddl == null ? null : ddl.trim();
            } finally {
                Resources.close(statement);
            }
        });
    }

    @Override
    public Element getComponentState() {
        return null;
    }

    @Override
    public void loadComponentState(@NotNull Element state) {

    }
}
