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

package com.dbn.execution.logging.ui;

import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Strings;
import com.dbn.common.util.Unsafe;
import com.dbn.connection.ConnectionHandler;
import com.dbn.execution.logging.LogOutput;
import com.dbn.execution.logging.LogOutputContext;
import com.intellij.diagnostic.logging.DefaultLogFilterModel;
import com.intellij.diagnostic.logging.LogConsoleBase;
import com.intellij.diagnostic.logging.LogFilterModel;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import java.io.StringReader;

public class DatabaseLoggingResultConsole extends LogConsoleBase{
    public static final StringReader EMPTY_READER = new StringReader("");
    public DatabaseLoggingResultConsole(@NotNull ConnectionHandler connection, String title, boolean buildInActions) {
        super(connection.getProject(), EMPTY_READER, title, buildInActions, createFilterModel(connection));
        getComponent().setBorder(Borders.lineBorder(JBColor.border(), 0, 0, 1, 0));
    }

    private static LogFilterModel createFilterModel(ConnectionHandler connection) {
        DefaultLogFilterModel defaultLogFilterModel = new DefaultLogFilterModel(connection.getProject());
        defaultLogFilterModel.setCheckStandartFilters(false);
        return defaultLogFilterModel;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    public void writeToConsole(LogOutputContext context, LogOutput output) {
        String text = output.getText();
        boolean isEmpty = Strings.isEmptyOrSpaces(text);
        boolean hideEmptyLines = context.isHideEmptyLines();

        if (!hideEmptyLines || !isEmpty) {
            writeToConsole(text + '\n', output.getType().getKey());
        }
    }

    @Override
    public ActionGroup getOrCreateActions() {
        return super.getOrCreateActions();
    }

    @Override
    public void dispose() {
        Unsafe.silent(() -> super.dispose());
    }
}
