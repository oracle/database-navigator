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

import com.dbn.common.locale.Formatter;
import com.dbn.connection.ConnectionHandler;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Data;
import lombok.Getter;

import java.util.Date;

@Data
public class LogOutput {

    @Getter
    public enum Type {
        SYS(ProcessOutputTypes.SYSTEM),
        STD(ProcessOutputTypes.STDOUT),
        ERR(ProcessOutputTypes.STDERR);
        private final Key<?> key;

        Type(Key<?> key) {
            this.key = key;
        }
    }

    private final String text;
    private final Type type;
    private final boolean scrollToEnd;
    private final boolean clearBuffer;

    private LogOutput(String text, Type type) {
        this(text, type, false, false);
    }
    private LogOutput(String text, Type type, boolean scrollToEnd, boolean clearBuffer) {
        this.text = text;
        this.type = type;
        this.scrollToEnd = scrollToEnd;
        this.clearBuffer = clearBuffer;
    }

    public static LogOutput createErrOutput(String text) {
        return new LogOutput(text, Type.ERR);
    }

    public static LogOutput createStdOutput(String text) {
        return new LogOutput(text, Type.STD);
    }

    public static LogOutput createSysOutput(String text) {
        return new LogOutput(text, Type.SYS);
    }

    public static LogOutput createSysOutput(LogOutputContext context, String message, boolean clearBuffer) {
        return createSysOutput(context, System.currentTimeMillis(), message, clearBuffer);
    }

    public static LogOutput createSysOutput(LogOutputContext context, long timestamp, String message, boolean clearBuffer) {
        ConnectionHandler connection = context.getConnection();
        Project project = connection.getProject();
        Formatter formatter = Formatter.getInstance(project);
        String date = formatter.formatDateTime(new Date(timestamp));
        String text = date + ": " + connection.getName();
        VirtualFile sourceFile = context.getSourceFile();
        if (sourceFile != null) {
            text += " / " + sourceFile.getName();
        }
        text += message;

        return new LogOutput(text, Type.SYS, true, clearBuffer);
    }

}
