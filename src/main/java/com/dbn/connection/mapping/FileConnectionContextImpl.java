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

package com.dbn.connection.mapping;

import com.dbn.common.file.util.VirtualFiles;
import com.dbn.common.util.Commons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.connection.SessionId;
import com.dbn.connection.session.DatabaseSession;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.options.setting.Settings.connectionIdAttribute;
import static com.dbn.common.options.setting.Settings.schemaIdAttribute;
import static com.dbn.common.options.setting.Settings.sessionIdAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.connection.ConnectionHandler.isLiveConnection;

@Slf4j
@Data
public class FileConnectionContextImpl implements FileConnectionContext {
    private String fileUrl = "";
    private ConnectionId connectionId;
    private SessionId sessionId = SessionId.MAIN;
    private SchemaId schemaId;

    FileConnectionContextImpl(){}

    public FileConnectionContextImpl(VirtualFile virtualFile){
        this.fileUrl = virtualFile.getUrl();
    }

    public FileConnectionContextImpl(String fileUrl, ConnectionId connectionId, SessionId sessionId, SchemaId schemaId) {
        this.fileUrl = fileUrl;
        this.connectionId = connectionId;
        this.sessionId = sessionId;
        this.schemaId = schemaId;
    }

    @Override
    public boolean setConnectionId(ConnectionId connectionId) {
        if (!Commons.match(this.connectionId, connectionId)) {
            this.connectionId = connectionId;
            return true;
        }
        return false;
    }

    @Override
    public boolean setSessionId(SessionId sessionId) {
        if (!Commons.match(this.sessionId, sessionId)) {
            this.sessionId = sessionId;
            return true;
        }
        return false;
    }

    @Override
    public boolean setSchemaId(SchemaId schemaId) {
        if (!Commons.match(this.schemaId, schemaId)) {
            this.schemaId = schemaId;
            return true;
        }
        return false;
    }

    @Override
    @Nullable
    public VirtualFile getFile() {
        return VirtualFiles.findFileByUrl(fileUrl);
    }

    @Override
    @Nullable
    public ConnectionHandler getConnection() {
        return ConnectionHandler.get(connectionId);
    }

    @Override
    @Nullable
    public DatabaseSession getSession() {
        ConnectionHandler connection = getConnection();
        if (isLiveConnection(connection)) {
            return connection.getSessionBundle().getSession(sessionId);
        }
        return null;
    }


    /*********************************************
     *            PersistentStateElement         *
     *********************************************/
    @Override
    public void readState(Element element) {
        fileUrl = stringAttribute(element, "file-url");

        if (fileUrl == null) {
            // TODO backward compatibility. Do cleanup
            fileUrl = stringAttribute(element, "file-path");
        }

        fileUrl = VirtualFiles.ensureFileUrl(fileUrl);

        connectionId = connectionIdAttribute(element, "connection-id");
        sessionId = sessionIdAttribute(element, "session-id", sessionId);
        schemaId = schemaIdAttribute(element, "current-schema");
    }

    @Override
    public void writeState(Element element) {
        element.setAttribute("file-url", fileUrl);
        element.setAttribute("connection-id", connectionId == null ? "" : connectionId.id());
        element.setAttribute("session-id", sessionId == null ? "" : sessionId.id());
        element.setAttribute("current-schema", schemaId == null ? "" : schemaId.id());
    }
}
