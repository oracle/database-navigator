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
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.util.Key;
import com.intellij.xdebugger.XDebugSession;

import java.util.Date;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class DBDebugConsoleLogger {
    protected XDebugSession session;

    public DBDebugConsoleLogger(XDebugSession session) {
        this.session = session;
    }

    public void system(String text) {
        log(text, MessageType.SYSTEM);
    }

    public void error(String text) {
        log(text, MessageType.ERROR);
    }

    public void info(String text) {
        log(text, MessageType.INFO);
    }

    public void warning(String text) {
        log(text, MessageType.WARNING);
    }


    private void log(String text, MessageType messageType) {
        try {
            RunContentDescriptor descriptor = session.getRunContentDescriptor();
            ProcessHandler processHandler = descriptor.getProcessHandler();
            if (processHandler == null) return;

            if (!processHandler.isStartNotified()) processHandler.startNotify();

            Formatter formatter = Formatter.getInstance(session.getProject());
            String date = formatter.formatDateTime(new Date());
            String prefix =
                    messageType == MessageType.ERROR ? "ERROR: " :
                    messageType == MessageType.WARNING ? "WARNING: " : "INFO: ";

            text = prefix + date + ": " + text + "\n";
            Key outputType =
                    messageType == MessageType.SYSTEM ? ProcessOutputTypes.SYSTEM :
                    messageType == MessageType.ERROR  ? ProcessOutputTypes.STDERR : ProcessOutputTypes.STDOUT;
            processHandler.notifyTextAvailable(text, outputType);

        } catch (IllegalStateException e) {
            conditionallyLog(e);
        }

    }
}
