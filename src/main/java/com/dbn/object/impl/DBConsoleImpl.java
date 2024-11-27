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

package com.dbn.object.impl;

import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.DBObjectMetadata;
import com.dbn.object.DBConsole;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectImpl;
import com.dbn.object.type.DBObjectType;
import com.dbn.vfs.DBConsoleType;
import com.dbn.vfs.file.DBConsoleVirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.SQLException;

import static com.dbn.common.dispose.Failsafe.nd;

@Getter
public class DBConsoleImpl extends DBObjectImpl<DBObjectMetadata> implements DBConsole {
    private final DBConsoleVirtualFile virtualFile;
    private final DBConsoleType consoleType;

    public DBConsoleImpl(@NotNull ConnectionHandler connection, String name, DBConsoleType consoleType) {
        super(connection, DBObjectType.CONSOLE, name);
        virtualFile = new DBConsoleVirtualFile(this);
        this.consoleType = consoleType;
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.CONSOLE;
    }

    @NotNull
    public DBConsoleVirtualFile getVirtualFile() {
        return nd(virtualFile);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBObjectMetadata metadata) throws SQLException {
        return null;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        switch (consoleType) {
            case STANDARD: return Icons.DBO_CONSOLE;
            case DEBUG: return Icons.DBO_CONSOLE_DEBUG;
        }
        return super.getIcon();
    }

    @Override
    public void setName(String newName) {
        ref().setObjectName(newName);
        virtualFile.setName(newName);
    }
}
