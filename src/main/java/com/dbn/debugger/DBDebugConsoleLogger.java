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

package com.dbn.debugger;

import com.dbn.common.locale.Formatter;
import com.dbn.common.message.MessageType;
import com.dbn.common.thread.Dispatch;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.xdebugger.XDebugSession;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

import static com.dbn.debugger.DBDebugUtil.isToolwindowSplit;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

public class DBDebugConsoleLogger {
    protected XDebugSession session;

    public DBDebugConsoleLogger(XDebugSession session) {
        this.session = session;
    }

    public void system(@Nls String text) {
        log(text, MessageType.SYSTEM);
    }

    public void error(@Nls String text) {
        log(text, MessageType.ERROR);
    }

    public void info(@Nls String text) {
        log(text, MessageType.INFO);
    }

    public void warning(@Nls String text) {
        log(text, MessageType.WARNING);
    }


    private void log(@Nls String text, MessageType messageType) {
        try {
            String log = prepareLogEntry(text, messageType);
            if (isToolwindowSplit()) {
                updateSplitConsole(messageType, log);
            } else {
                updateLegacyConsole(messageType, log);
            }
        } catch (IllegalStateException e) {
            conditionallyLog(e);
        }
    }

    private @Nls @NotNull String prepareLogEntry(@Nls String text, MessageType messageType) {
        Project project = session.getProject();
        Formatter formatter = Formatter.getInstance(project);
        String date = formatter.formatDateTime(new Date());
        String prefix =
                messageType == MessageType.ERROR ? txt("log.debugger.token.Error") :
                messageType == MessageType.WARNING ? txt("log.debugger.token.Warning") :
                        txt("log.debugger.token.Info");

        text = txt("log.debugger.text.ConsoleEntry", prefix, date, text) + "\n";
        return text;
    }

    private void updateLegacyConsole(MessageType messageType, @Nls String text) {
        ProcessHandler processHandler = session.getDebugProcess().getProcessHandler();
        if (!processHandler.isStartNotified()) {
            processHandler.startNotify();
        }
        Key outputType =
                messageType == MessageType.ERROR  ? ProcessOutputTypes.STDERR :
                messageType == MessageType.SYSTEM ? ProcessOutputTypes.SYSTEM :
                ProcessOutputTypes.STDOUT;
        processHandler.notifyTextAvailable(text, outputType);
    }

    private void updateSplitConsole(MessageType messageType, @Nls String text) {
        ConsoleView consoleView = session.getConsoleView();
        if (consoleView == null) return;
        ConsoleViewContentType contentType =
                messageType == MessageType.ERROR ? ConsoleViewContentType.ERROR_OUTPUT :
                messageType == MessageType.SYSTEM ? ConsoleViewContentType.SYSTEM_OUTPUT :
                ConsoleViewContentType.NORMAL_OUTPUT;

        Dispatch.run(() -> consoleView.print(text, contentType));
    }
}
