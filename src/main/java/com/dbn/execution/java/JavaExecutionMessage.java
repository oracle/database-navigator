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

package com.dbn.execution.java;

import com.dbn.common.message.MessageType;
import com.dbn.connection.ConnectionId;
import com.dbn.database.common.execution.JavaExecutionProcessor;
import com.dbn.editor.DBContentType;
import com.dbn.execution.common.message.ConsoleMessage;
import com.dbn.object.DBJavaMethod;
import com.dbn.vfs.file.DBContentVirtualFile;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class JavaExecutionMessage extends ConsoleMessage {
    private JavaExecutionProcessor executionProcessor;
    private DBEditableObjectVirtualFile databaseFile;
    private DBContentVirtualFile contentFile;
    private DBContentType contentType;
    private ConnectionId connectionId;

    public JavaExecutionMessage(JavaExecutionProcessor executionProcessor, String message, MessageType messageType) {
        super(messageType, message);
        this.executionProcessor = executionProcessor;
        this.connectionId = executionProcessor.getMethod().getConnectionId();
    }

    public DBEditableObjectVirtualFile getDatabaseFile() {
        if (databaseFile == null) {
            DBJavaMethod method = executionProcessor.getMethod();
//            databaseFile = method.getEditableVirtualFile();
            return null;
        }
        return databaseFile;
    }

    @Nullable
    public DBContentVirtualFile getContentFile() {
        if (contentFile == null) {
            DBEditableObjectVirtualFile databaseFile = getDatabaseFile();
            contentFile = databaseFile.getContentFile(contentType);
        }
        return contentFile;
    }
}
