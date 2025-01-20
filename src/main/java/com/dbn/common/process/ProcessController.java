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

package com.dbn.common.process;

import com.dbn.common.thread.Background;
import com.dbn.common.util.Chars;
import com.dbn.common.util.Unsafe;
import lombok.Getter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.dbn.common.util.Streams.bufferedReader;
import static com.dbn.common.util.Streams.bufferedWriter;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.common.util.TimeUtil.isOlderThan;
import static com.dbn.common.util.Unsafe.warned;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.concurrent.locks.LockSupport.parkNanos;

/**
 * Wrapper for a {@link Process} providing control over the input and output streams.
 * It starts monitoring the input stream (the STDOUT ot the Process) in a separate thread and
 * passes the content to an external consumer.
 *
 * @author Dan Cioca (Oracle)
 */
@Getter
public class ProcessController {
    private Process process;
    private final BufferedReader outputReader;
    private final BufferedWriter inputWriter;
    private Consumer<String> logConsumer = s -> {};

    private long cmdTimestamp = System.currentTimeMillis();
    private long logTimestamp = System.currentTimeMillis();

    private final long timeout;

    public ProcessController(Process process, int timeout, TimeUnit timeoutUnit) {
        this.process = process;
        this.inputWriter = bufferedWriter(process.getOutputStream());
        this.outputReader = bufferedReader(process.getInputStream());
        this.timeout = timeoutUnit.toMillis(timeout);
    }

    public void init(Consumer<String> logConsumer) {
        this.logConsumer = logConsumer;
        Background.run(() -> readOutput());

        parkNanos(SECONDS.toNanos(1));
    }

    private void readOutput() {
        // read from the process output streams
        String line = readLine();
        while (line != null) {
            logConsumer.accept(line);
            logTimestamp = System.currentTimeMillis();
            line = readLine();
        }

        // release the process if older than the given idle timeout
        if (isOlderThan(logTimestamp, timeout)) {
            destroy();
        }
    }

    private String readLine() {
        return Unsafe.silent(null, () -> outputReader.readLine());
    }

    private boolean checkAlive() {
        Process process = this.process;
        if (process == null) return false;
        if (process.isAlive()) return true;

        destroy();
        return false;
    }

    public void sendCommands(List<String> command) {
        command.forEach(c -> sendCommand(c));
    }

    public void sendCommand(String command) {
        writeCommand(command);
        waitForOutput();
    }

    public void sendPassword(char[] password) {
        writeCommand(Chars.toString(password));
    }

    private void waitForOutput() {
        if (!checkAlive())  return;
        while (!isOlderThan(Math.max(cmdTimestamp, logTimestamp), 1, SECONDS)) {
            if (!checkAlive())  return;
            parkNanos(MILLISECONDS.toNanos(500));
        }
    }

    private void writeCommand(String command) {
        if (!checkAlive())  return;
        if (isEmpty(command)) return;

        warned(() -> {
            cmdTimestamp = System.currentTimeMillis();
            inputWriter.write(command);
            inputWriter.newLine();
            inputWriter.flush();
        });
    }

    public void destroy() {
        Process process = this.process;
        if (process == null) return;

        this.process = null;

        warned(() -> process.destroy());
        warned(() -> inputWriter.close());
        warned(() -> outputReader.close());
    }

    public void waitFor() {
        Process process = this.process;
        if (process == null) return;

        // allow the process to finish
        parkNanos(SECONDS.toNanos(5));
        warned(() -> process.waitFor(timeout, MILLISECONDS));
    }

}
