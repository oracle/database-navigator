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

package com.dbn.execution.compiler;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.message.MessageType;
import com.dbn.connection.ConnectionId;
import com.dbn.editor.DBContentType;
import com.dbn.execution.common.message.ConsoleMessage;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.vfs.file.DBContentVirtualFile;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

@Getter
@Setter
public class CompilerMessage extends ConsoleMessage implements Comparable<CompilerMessage> {
    private final CompilerResult compilerResult;
    private final DBContentType contentType;
    private String subjectIdentifier;
    private int line;
    private int position;
    private boolean echo;
    private DBEditableObjectVirtualFile databaseFile;
    private DBContentVirtualFile contentFile;

    public CompilerMessage(CompilerResult compilerResult, DBContentType contentType, String text, MessageType type) {
        super(type, text);
        this.compilerResult = compilerResult;
        this.contentType = contentType;
    }

    public CompilerMessage(CompilerResult compilerResult, DBContentType contentType, String text) {
        super(MessageType.INFO, text);
        this.compilerResult = compilerResult;
        this.contentType = contentType;
    }

    public CompilerMessage(CompilerResult compilerResult, ResultSet resultSet) throws SQLException {
        super(MessageType.ERROR, resultSet.getString("TEXT"));
        line = resultSet.getInt("LINE");
        position = resultSet.getInt("POSITION");

        line = Math.max(line-1, 0);
        position = Math.max(position-1, 0);
        this.compilerResult = compilerResult;

        DBContentType objectContentType = DBContentType.get(compilerResult.getObjectType());
        if (objectContentType == DBContentType.CODE_SPEC_AND_BODY) {
            String objectType = resultSet.getString("OBJECT_TYPE");
            contentType = objectType.contains("BODY") ?  DBContentType.CODE_BODY : DBContentType.CODE_SPEC;
        } else {
            contentType = objectContentType;
        }

        echo = !text.startsWith("PLS") && !text.contains("ORA");
        if (echo) {
            setType(MessageType.WARNING);
        }

        subjectIdentifier = extractIdentifier(text, '\'');
        if (subjectIdentifier == null) subjectIdentifier = extractIdentifier(text, '"');
    }

    private static String extractIdentifier(String message, char identifierQuoteChar) {
        int startIndex = message.indexOf(identifierQuoteChar);
        if (startIndex > -1) {
            startIndex = startIndex + 1;
            int endIndex = message.indexOf(identifierQuoteChar, startIndex);
            if (endIndex > -1) {
                return message.substring(startIndex, endIndex);
            }
        }
        return null;
    }

    @Nullable
    public DBEditableObjectVirtualFile getDatabaseFile() {
        DBSchemaObject schemaObject = compilerResult.getObject();
        if (databaseFile == null && schemaObject != null) {
            databaseFile = schemaObject.getEditableVirtualFile();
        }
        return databaseFile;
    }

    @Nullable
    public DBContentVirtualFile getContentFile() {
        if (contentFile == null) {
            DBEditableObjectVirtualFile databaseFile = getDatabaseFile();
            if (databaseFile != null) {
                contentFile = databaseFile.getContentFile(contentType);
            }
        }
        return contentFile;
    }

    public DBSchemaObject getObject() {
        return compilerResult.getObject();
    }

    @Nullable
    @Override
    public ConnectionId getConnectionId() {
        return compilerResult.getConnectionId();
    }

    public Project getProject() {
        return compilerResult.getProject();
    }

    public String getObjectName() {
        return compilerResult.getObjectRef().getObjectName();
    }

    @Override
    public int compareTo(CompilerMessage that) {
        if (this.getType() == that.getType()) {
            return line - that.line;
        }
        return that.getType().compareTo(this.getType());
    }

    public boolean isSameResult(CompilerMessage that) {
        return this.getCompilerResult() == that.getCompilerResult();
    }
    public boolean isSameTarget(CompilerMessage that) {
        if (this.contentType != that.contentType) return false;

        var thisObject = this.compilerResult.getObjectRef();
        var thatObject = that.compilerResult.getObjectRef();
        if (!Objects.equals(thisObject, thatObject)) return false;

        return true;
    }

    @Override
    public void disposeInner() {
        Disposer.dispose(compilerResult);
        super.disposeInner();
    }
}
