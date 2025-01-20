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

package com.dbn.assistant.interceptor;

import com.dbn.assistant.DatabaseAssistantManager;
import com.dbn.common.exception.ProcessDeferredException;
import com.dbn.common.interceptor.Interceptor;
import com.dbn.common.interceptor.InterceptorType;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.interceptor.DatabaseInterceptorType;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseAssistantInterface;
import com.dbn.execution.statement.StatementExecutionContext;
import com.dbn.execution.statement.StatementExecutionInput;
import com.dbn.language.common.element.ElementType;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.ExecutablePsiElement;
import com.dbn.object.DBAIProfile;
import com.intellij.openapi.project.Project;
import lombok.SneakyThrows;

import static com.dbn.database.DatabaseFeature.AI_ASSISTANT;

public class StatementExecutionInterceptor implements Interceptor<StatementExecutionContext> {
    public static final StatementExecutionInterceptor INSTANCE = new StatementExecutionInterceptor();

    private StatementExecutionInterceptor() {}

    @Override
    public InterceptorType<?> getType() {
        return DatabaseInterceptorType.STATEMENT_EXECUTION;
    }

    @Override
    public boolean supports(StatementExecutionContext context) {
        StatementExecutionInput input = context.getInput();
        ConnectionHandler connection = input.getConnection();
        if (connection == null) return false;
        if (AI_ASSISTANT.isNotSupported(connection)) return false;

        ExecutablePsiElement element = input.getExecutablePsiElement();
        if (element == null) return false;

        ElementType specificElementType = element.getSpecificElementType(true);
        return specificElementType.is(ElementTypeAttribute.DB_ASSISTANT);
    }

    @Override
    @SneakyThrows
    public void before(StatementExecutionContext context) {
        StatementExecutionInput input = context.getInput();
        ConnectionHandler connection = input.getConnection();
        if (connection == null) return;

        DBNConnection conn = context.getConnection();
        if (conn == null) return;

        Project project = context.getProject();
        DatabaseAssistantManager assistantManager = DatabaseAssistantManager.getInstance(project);

        ConnectionId connectionId = connection.getConnectionId();
        DBAIProfile profile = assistantManager.getDefaultProfile(connectionId);

        if (profile == null) {
            assistantManager.initializeAssistant(connectionId);
            throw new ProcessDeferredException("Assistant not initialized");
        }

        DatabaseAssistantInterface assistantInterface = connection.getAssistantInterface();
        assistantInterface.setCurrentProfile(conn, profile.getName(true));
    }
}
