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

package com.dbn.vfs.file;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.vfs.DBVirtualFileBase;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class DBConnectionVirtualFile extends DBVirtualFileBase {
    private final ConnectionRef connection;

    public DBConnectionVirtualFile(ConnectionHandler connection) {
        super(connection.getProject(), connection.getName());
        this.connection = connection.ref();
    }

    @Override
    @NotNull
    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    /*********************************************************
     *                     VirtualFile                       *
     *********************************************************/

    @Override
    public boolean isValid() {
        return connection.isValid();
    }

    @Override
    public Icon getIcon() {
        return getConnection().getIcon();
    }

    @Override
    public String getExtension() {
        return null;
    }
}

