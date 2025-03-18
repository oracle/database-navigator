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

package com.dbn.execution.script;

import com.dbn.common.routine.Consumer;
import com.dbn.common.thread.Threads;
import com.dbn.common.util.Chars;
import com.dbn.database.CmdLineExecutionInput;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.util.List;

import static com.dbn.common.util.Streams.bufferedWriter;
import static com.dbn.common.util.Unsafe.warned;

/**
 * Wrapper for a {@link Process} extending {@link OSProcessHandler}, providing control
 * over the input and output streams of the process, as well invoking custom functionality
 * during different phases of the process lifecycle, by using registered event consumers ({@link Consumer<ProcessEvent>}).
 *
 * @author Dan Cioca (Oracle)
 */
public final class ScriptExecutionProcessHandler extends OSProcessHandler {
    private final CmdLineExecutionInput input;
    private Consumer<ProcessEvent> outputConsumer;
    private Consumer<ProcessEvent> notifiedConsumer;
    private Consumer<ProcessEvent> terminatingConsumer;
    private Consumer<ProcessEvent> terminatedConsumer;
    private final BufferedWriter writer;
    private boolean authenticated;
    private Runnable initializer;

    private ScriptExecutionProcessHandler(CmdLineExecutionInput input) throws ExecutionException {
        super(input.getCommand());
        this.input = input;
        this.writer = bufferedWriter(getProcessInput());

        addProcessListener(createProcessListener());

        // if input promotes the password, authentication is yet to be done
        authenticated = Chars.isEmpty(input.getPassword());
    }

    private @NotNull ProcessAdapter createProcessListener() {
        return new ProcessAdapter() {
            @Override
            public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                ensureInitialized();
                consumeEvent(outputConsumer, event);
            }

            @Override
            public void startNotified(@NotNull ProcessEvent event) {
                initialize();
                consumeEvent(notifiedConsumer, event);
            }

            @Override
            public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
                consumeEvent(terminatingConsumer, event);
            }

            @Override
            public void processTerminated(@NotNull ProcessEvent event) {
                warned(() -> writer.close());
                consumeEvent(terminatedConsumer, event);
            }
        };
    }

    private synchronized void initialize() {
        if (authenticated) {
            sendInputCommands();
            return;
        }

        authenticated = true;
        sendPassword();
        initializer = () -> sendInputCommands();
        Threads.sleep(500);
    }

    private synchronized void ensureInitialized() {
        if (initializer == null) return;
        Runnable initializer = this.initializer;
        this.initializer = null;
        initializer.run();
    }

    private void consumeEvent(Consumer<ProcessEvent> consumer, @NotNull ProcessEvent event) {
        if (consumer != null) consumer.accept(event);
    }

    public void whenNotified(Consumer<ProcessEvent> notifiedConsumer) {
        this.notifiedConsumer = notifiedConsumer;
    }

    public void whenTerminating(Consumer<ProcessEvent> terminatingConsumer) {
        this.terminatingConsumer = terminatingConsumer;
    }

    public void whenTerminated(Consumer<ProcessEvent> terminatedConsumer) {
        this.terminatedConsumer = terminatedConsumer;
    }

    public void whenOutputted(Consumer<ProcessEvent> outputConsumer) {
        this.outputConsumer = outputConsumer;
    }

    public void sendCommand(String command) {
        if (isProcessTerminated()) return;
        if (isProcessTerminating()) return;

        warned(() -> {
            writer.write(command);
            writer.newLine();
            writer.flush();
        });
    }

    private void sendPassword() {
        String password = Chars.toString(input.getPassword());
        sendCommand(password);
    }

    public void sendInputCommands() {
        sendCommands(input.getStatements());
    }

    public void sendCommands(List<String> commands) {
        commands.forEach(c -> sendCommand(c));
    }

    public static ScriptExecutionProcessHandler startProcess(CmdLineExecutionInput input) throws ExecutionException {
        return new ScriptExecutionProcessHandler(input);
    }
}
