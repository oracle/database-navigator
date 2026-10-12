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

package com.dbn.connection.console;

import com.dbn.common.dispose.DisposableContainers;
import com.dbn.connection.ConnectionComponentBase;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.DBConsole;
import com.dbn.object.event.ObjectChangeAction;
import com.dbn.object.event.ObjectChangeEvent;
import com.dbn.object.impl.DBConsoleImpl;
import com.dbn.vfs.DBConsoleType;
import com.dbn.vfs.file.DBConsoleVirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.object.event.ObjectChangeAction.CREATE;
import static com.dbn.object.event.ObjectChangeAction.DELETE;
import static com.dbn.object.event.ObjectChangeAction.UPDATE;
import static com.dbn.object.type.DBObjectType.CONSOLE;

@Getter
public class DatabaseConsoleBundle extends ConnectionComponentBase {
    private final List<DBConsole> consoles = DisposableContainers.concurrentList(this);

    public DatabaseConsoleBundle(ConnectionHandler connection) {
        super(connection);
        getDefaultConsole();
    }

    public Set<String> getConsoleNames() {
        Set<String> consoleNames = new HashSet<>();
        for (DBConsole console : consoles) {
            consoleNames.add(console.getName());
        }

        return consoleNames;
    }

    @NotNull
    public DBConsole getDefaultConsole() {
        return getConsole(getConnection().getName(), DBConsoleType.STANDARD, true);
    }

    @Nullable
    public DBConsole getConsole(String name) {
        for (DBConsole console : consoles) {
            if (Objects.equals(console.getName(), name)) {
                return console;
            }
        }
        return null;
    }

    @NotNull
    public DBConsole ensureConsole(String name) {
        DBConsole console = getConsole(name);
        return nd(console);
    }

    public DBConsole getConsole(String name, DBConsoleType type, boolean create) {
        DBConsole console = getConsole(name);
        if (console != null) return console;
        if (!create) return null;

        return createConsole(name, type);
    }

    DBConsole restoreConsole(String name, DBConsoleType type) {
        DBConsole console = getConsole(name);
        if (console != null) return console;

        return createConsole(name, type, false, false);
    }

    void clear() {
        consoles.clear();
    }

    DBConsole createConsole(String name, DBConsoleType type) {
        return createConsole(name, type, true, true);
    }

    private DBConsole createConsole(String name, DBConsoleType type, boolean initialize, boolean notify) {
        ConnectionHandler connection = getConnection();
        DBConsole console = new DBConsoleImpl(connection, name, type);
        consoles.add(console);
        Collections.sort(consoles);

        if (initialize) {
            DBConsoleVirtualFile virtualFile = console.getVirtualFile();
            virtualFile.setDatabaseSchema(connection.getDefaultSchemaId());
        }

        if (notify) {
            notifyChanges(CREATE);
        }

        return console;
    }

    void removeConsole(DBConsole console) {
        if (console == null) return;
        if (!consoles.remove(console)) return;

        notifyChanges(DELETE);
    }

    @Override
    public void disposeInner() {
    }

    void renameConsole(String oldName, String newName) {
        if (Objects.equals(oldName, newName)) return;

        DBConsole console = ensureConsole(oldName);
        console.setName(newName);
        notifyChanges(UPDATE);
    }

    void notifyChanges(ObjectChangeAction action) {
        ConnectionHandler connection = getConnection();
        ObjectChangeEvent.notify(action, CONSOLE, connection.getConnectionId(), null);
    }
}
