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

package com.dbn.execution;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SessionId;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.connection.session.DatabaseSessionBundle;
import com.dbn.database.DatabaseFeature;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.sessionIdAttribute;
import static com.dbn.common.options.setting.Settings.setBooleanAttribute;
import static com.dbn.execution.ExecutionOption.COMMIT_AFTER_EXECUTION;
import static com.dbn.execution.ExecutionOption.CONTEXT_EXPANDED;
import static com.dbn.execution.ExecutionOption.ENABLE_LOGGING;

@Getter
@Setter
public abstract class LocalExecutionInput extends ExecutionInput{
    private ExecutionOptions options = new ExecutionOptions();
    private SessionId targetSessionId = SessionId.MAIN;

    public LocalExecutionInput(Project project, ExecutionTarget executionTarget) {
        super(project, executionTarget);

        ConnectionHandler connection = getConnection();
        if (connection != null) {
            if (DatabaseFeature.DATABASE_LOGGING.isSupported(connection)) {
                options.set(ENABLE_LOGGING, connection.isLoggingEnabled());
            }
        }
    }

    public void setTargetSession(DatabaseSession databaseSession) {
        setTargetSessionId(databaseSession == null ? SessionId.MAIN : databaseSession.getId());
    }

    public String getTargetSessionName() {
        ConnectionHandler connection = getConnection();
        if (connection == null) return "Main";

        DatabaseSessionBundle sessionBundle = connection.getSessionBundle();
        return sessionBundle.getSessionName(targetSessionId);
    }

    public abstract boolean hasExecutionVariables();

    public abstract boolean isSchemaSelectionAllowed();

    public abstract boolean isSessionSelectionAllowed();

    public abstract boolean isDatabaseLogProducer();

    public void setContextExpanded(boolean expanded) {
        options.set(CONTEXT_EXPANDED, expanded);
    }

    public boolean isContextExpanded() {
        return options.is(CONTEXT_EXPANDED);
    }

    /*********************************************************
     *                 PersistentConfiguration               *
     *********************************************************/
    @Override
    public void readConfiguration(Element element) {
        super.readConfiguration(element);
        targetSessionId = sessionIdAttribute(element, "session-id", targetSessionId);
        options.set(CONTEXT_EXPANDED, booleanAttribute(element, "context-expanded", false));
        options.set(ENABLE_LOGGING, booleanAttribute(element, "enable-logging", true));
        options.set(COMMIT_AFTER_EXECUTION, booleanAttribute(element, "commit-after-execution", true));
    }

    @Override
    public void writeConfiguration(Element element) {
        super.writeConfiguration(element);
        element.setAttribute("session-id", targetSessionId.id());
        setBooleanAttribute(element, "context-expanded", options.is(CONTEXT_EXPANDED));
        setBooleanAttribute(element, "enable-logging", options.is(ENABLE_LOGGING));
        setBooleanAttribute(element, "commit-after-execution", options.is(COMMIT_AFTER_EXECUTION));
    }
}
