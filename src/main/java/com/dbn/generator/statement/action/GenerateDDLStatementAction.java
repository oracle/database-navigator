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

package com.dbn.generator.statement.action;

import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.ddl.DDLManager;
import com.dbn.generator.statement.StatementGeneratorResult;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class GenerateDDLStatementAction extends GenerateStatementAction {
    private final DBObjectRef object;

    GenerateDDLStatementAction(DBObject object) {
        this.object = DBObjectRef.of(object);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        e.getPresentation().setText("DDL Statement");
    }

    @Nullable
    @Override
    public ConnectionHandler getConnection() {
        return getObject().getConnection();
    }

    @NotNull
    public DBObject getObject() {
        return DBObjectRef.ensure(object);
    }

    @Override
    protected StatementGeneratorResult generateStatement(Project project) {
        StatementGeneratorResult result = new StatementGeneratorResult();
        DBObject object = getObject();
        try {
            DDLManager ddlManager = DDLManager.getInstance(project);
            String statement = ddlManager.extractDDL(object);
            if (Strings.isEmptyOrSpaces(statement)) {
                String message =
                        "Could not extract DDL statement for " + object.getQualifiedNameWithType() + ".\n" +
                                "You may not have enough rights to perform this action. Please contact your database administrator for more details.";
                result.getMessages().addErrorMessage(message);
            }

            result.setStatement(statement);
        } catch (SQLException e) {
            conditionallyLog(e);
            result.getMessages().addErrorMessage(
                    "Could not extract DDL statement for " + object.getQualifiedNameWithType() + ".\n" +
                            "Cause: " + e.getMessage());
        }
        return result;
    }
}
