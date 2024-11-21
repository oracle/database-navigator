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

package com.dbn.execution.logging;

import com.dbn.common.action.DataKeys;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.icon.Icons;
import com.dbn.common.util.Commons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.dbn.execution.ExecutionResultBase;
import com.dbn.execution.logging.ui.DatabaseLoggingResultConsole;
import com.dbn.execution.logging.ui.DatabaseLoggingResultForm;
import com.dbn.language.common.DBLanguagePsiFile;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import static com.dbn.common.dispose.Checks.isNotValid;

public class DatabaseLoggingResult extends ExecutionResultBase<DatabaseLoggingResultForm> {

    private LogOutputContext context;

    public DatabaseLoggingResult(@NotNull LogOutputContext context) {
        this.context = context;
    }

    @Nullable
    @Override
    public DatabaseLoggingResultForm createForm() {
        return new DatabaseLoggingResultForm(this);
    }

    @NotNull
    public LogOutputContext getContext() {
        return Failsafe.nn(context);
    }

    @Override
    @NotNull
    public String getName() {
        ConnectionHandler connection = getConnection();
        VirtualFile sourceFile = context.getSourceFile();
        if (sourceFile == null) {
            DatabaseCompatibilityInterface compatibility = connection.getCompatibilityInterface();
            String databaseLogName = compatibility.getDatabaseLogName();

            return connection.getName() + " - " + Commons.nvl(databaseLogName, "Log Output");
        } else {
            return connection.getName() + " - " + sourceFile.getName();
        }
    }

    public boolean matches(LogOutputContext context) {
        return this.context == context || this.context.matches(context);
    }

    @Override
    public Icon getIcon() {
        return Icons.EXEC_LOG_OUTPUT_CONSOLE;
    }

    @NotNull
    @Override
    public Project getProject() {
        return getConnection().getProject();
    }

    @Override
    public ConnectionId getConnectionId() {
        return context.getConnectionId();
    }

    @Nullable
    public VirtualFile getSourceFile() {
        return context.getSourceFile();
    }

    @NotNull
    @Override
    public ConnectionHandler getConnection() {
        return context.getConnection();
    }

    @Override
    public DBLanguagePsiFile createPreviewFile() {
        return null;
    }

    public void write(LogOutputContext context, LogOutput output) {
        this.context = context;
        DatabaseLoggingResultForm resultForm = getForm();
        if (isNotValid(resultForm)) return;

        DatabaseLoggingResultConsole console = resultForm.getConsole();
        if (output.isClearBuffer()) {
            console.clear();
        }
        if (output.isScrollToEnd()) {
            ConsoleView consoleView = console.getConsole();
            if (consoleView instanceof ConsoleViewImpl) {
                ConsoleViewImpl consoleViewImpl = (ConsoleViewImpl) consoleView;
                consoleViewImpl.requestScrollingToEnd();
            }
        }
        console.writeToConsole(context, output);
    }

    /********************************************************
     *                    Data Provider                     *
     ********************************************************/
    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.DATABASE_LOG_OUTPUT.is(dataId)) return this;
        return null;
    }


}
