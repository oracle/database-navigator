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

import com.dbn.common.ref.WeakRef;
import com.dbn.common.util.Commons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionRef;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class LogOutputContext {
    public enum Status{
        NEW,
        ACTIVE,
        FINISHED,    // finished normally (or with error)
        STOPPED,     // interrupted by user
        CLOSED      // cancelled completely (console closed)
    }
    private final ConnectionRef connection;
    private final WeakRef<VirtualFile> sourceFile;
    private WeakRef<Process> process;
    private Status status = Status.NEW;
    private boolean hideEmptyLines = false;

    public LogOutputContext(@NotNull ConnectionHandler connection) {
        this(connection, null, null);
    }

    public LogOutputContext(@NotNull ConnectionHandler connection, @Nullable VirtualFile sourceFile, @Nullable Process process) {
        this.connection = connection.ref();
        this.sourceFile = WeakRef.of(sourceFile);
        this.process = WeakRef.of(process);
    }

    @NotNull
    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @Nullable
    public VirtualFile getSourceFile() {
        return WeakRef.get(sourceFile);
    }

    @Nullable
    public Process getProcess() {
        return WeakRef.get(process);
    }

    public void setProcess(Process process) {
        this.process = WeakRef.of(process);
    }

    public boolean isProcessAlive() {
        Process process = getProcess();
        if (process != null) {
            if (process.isAlive()) {
                return true;
            }

            // legacy
            try {
                process.exitValue();
            } catch(IllegalThreadStateException e) {
                return true;
            }
        }
        return false;
    }

    public boolean matches(LogOutputContext context) {
        return getConnection() == context.getConnection() &&
                Commons.match(getSourceFile(), context.getSourceFile());
    }

    public void start() {
        status = Status.ACTIVE;
    }

    public void finish() {
        if (status == Status.ACTIVE) {
            status = Status.FINISHED;
        }
        destroyProcess();
    }


    public void stop() {
        if (status == Status.ACTIVE) {
            status = Status.STOPPED;
        }
        destroyProcess();
    }


    public void close() {
        status = Status.CLOSED;
        destroyProcess();
    }

    public boolean isActive() {
        if (status == Status.ACTIVE && !isProcessAlive()) {
            finish();
        }
        return status == Status.ACTIVE;
    }

    public boolean isClosed() {
        return status == Status.CLOSED;
    }

    public boolean isStopped() {
        return status == Status.STOPPED;
    }


    private void destroyProcess() {
        Process process = getProcess();
        if (process != null) {
            process.destroy();
        }
    }

    public ConnectionId getConnectionId() {
        return connection.getConnectionId();
    }
}
