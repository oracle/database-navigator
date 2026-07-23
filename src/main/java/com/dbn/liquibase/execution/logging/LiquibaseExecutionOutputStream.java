/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.liquibase.execution.logging;

import com.dbn.liquibase.operation.LiquibaseOperationResult;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/** Forwards Liquibase command output to a DBN execution result. */
public class LiquibaseExecutionOutputStream extends OutputStream {
    private final LiquibaseOperationResult result;
    private final Consumer<String> sqlConsumer;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    public LiquibaseExecutionOutputStream(
            @NotNull LiquibaseOperationResult result,
            Consumer<String> sqlConsumer) {
        this.result = result;
        this.sqlConsumer = sqlConsumer;
    }

    @Override
    public synchronized void write(int value) {
        buffer.write(value);
        if (value == '\n') flushBuffer();
    }

    @Override
    public synchronized void write(byte @NotNull [] bytes, int offset, int length) {
        buffer.write(bytes, offset, length);
        flushLines();
    }

    @Override
    public synchronized void flush() {
        flushBuffer();
    }

    private void flushLines() {
        byte[] bytes = buffer.toByteArray();
        int lineEnd = -1;
        for (int index = bytes.length - 1; index >= 0; index--) {
            if (bytes[index] == '\n') {
                lineEnd = index;
                break;
            }
            if (bytes[index] != '\r') break;
        }
        if (lineEnd < 0) return;

        byte[] line = new byte[lineEnd + 1];
        System.arraycopy(bytes, 0, line, 0, line.length);
        buffer.reset();
        buffer.write(bytes, lineEnd + 1, bytes.length - lineEnd - 1);
        appendOutput(new String(line, StandardCharsets.UTF_8));
    }

    private void flushBuffer() {
        if (buffer.size() == 0) return;
        String text = buffer.toString(StandardCharsets.UTF_8);
        buffer.reset();
        appendOutput(text);
    }

    private void appendOutput(@NotNull String text) {
        if (sqlConsumer != null) sqlConsumer.accept(text);
        result.appendConsoleOutput(text);
    }

    @Override
    public synchronized void close() throws IOException {
        flushBuffer();
    }
}
