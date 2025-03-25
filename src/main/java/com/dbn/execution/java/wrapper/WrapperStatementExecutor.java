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

package com.dbn.execution.java.wrapper;

import com.dbn.common.Priority;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.jdbc.DBNPreparedStatement;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.object.DBJavaMethod;
import com.intellij.openapi.project.Project;

import java.sql.SQLException;
import java.util.List;

public class WrapperStatementExecutor {

    public Wrapper createExecutionWrappers(DBJavaMethod method, boolean useFriendlyNames) throws SQLException {
        Project project = method.getProject();

        WrapperBuilder wrapperBuilder = WrapperBuilder.getInstance();
        Wrapper wrapper = wrapperBuilder.build(method, useFriendlyNames);

        WrapperStatementBuilder statementBuilder = new WrapperStatementBuilder(project);
        String creationStatement = statementBuilder.buildWrapperCreationStatement(wrapper);

        ConnectionId connectionId = method.getConnectionId();
        DatabaseInterfaceInvoker.execute(Priority.HIGH,
                "Creating execution wrappers",
                "Creating java execution wrappers for " + method.getPresentableText(),
                project,
                connectionId, c -> {
                    DBNPreparedStatement statement = c.prepareStatement(creationStatement);
                    statement.execute();
                });

        return wrapper;
    }

    public Wrapper createExecutionWrappers(List<DBJavaMethod> methods, boolean useFriendlyNames) throws SQLException {
        if(methods.isEmpty()) return null;
        Project project = methods.get(0).getProject();

        WrapperBuilder wrapperBuilder = WrapperBuilder.getInstance();
        Wrapper wrapper = wrapperBuilder.build(methods, useFriendlyNames);

        WrapperStatementBuilder statementBuilder = new WrapperStatementBuilder(project);
        String creationStatement = statementBuilder.buildWrapperCreationStatement(wrapper);

        ConnectionId connectionId = methods.get(0).getConnectionId();
        DatabaseInterfaceInvoker.execute(Priority.HIGH,
                "Creating execution wrappers",
                "Creating java execution wrappers for selected method",
                project,
                connectionId, c -> {
                    DBNPreparedStatement statement = c.prepareStatement(creationStatement);
                    statement.execute();
                });

        return wrapper;
    }

    public void discardExecutionWrappers(DBJavaMethod method, Wrapper wrapper) throws SQLException {
        Project project = method.getProject();
        WrapperStatementBuilder statementBuilder = new WrapperStatementBuilder(project);
        String removalStatement = statementBuilder.buildWrapperRemovalStatement(wrapper);

        ConnectionId connectionId = method.getConnectionId();
        DatabaseInterfaceInvoker.execute(Priority.HIGH,
                "Removing execution wrappers",
                "Removing java execution wrappers for " + method.getPresentableText(),
                project,
                connectionId, c -> {
                    DBNPreparedStatement statement = c.prepareStatement(removalStatement);
                    statement.execute();
                });
    }
}
