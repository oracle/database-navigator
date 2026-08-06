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

package com.dbn.mcp.registry;

import com.dbn.common.event.ProjectEvents;
import com.intellij.openapi.project.Project;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.mcp.build.McpDistPaths.BUILD_LOG;

/**
 * Output of server operations that are running now, held per server until the build or deployment
 * step ends and the log is written to disk. Bounded, because a native-image build can produce a
 * lot of output and this is only ever a live view - the file remains the durable record.
 */
@RequiredArgsConstructor
public class McpBuildLogs {
    private static final int MAX_CHARS = 512 * 1024;

    private final Project project;
    private final Map<String, StringBuilder> logs = new ConcurrentHashMap<>();

    public void append(@NotNull String outputDirectory, @NotNull String line) {
        StringBuilder log = logs.computeIfAbsent(outputDirectory, k -> new StringBuilder());
        synchronized (log) {
            log.append(line).append('\n');
            if (log.length() > MAX_CHARS) log.delete(0, log.length() - MAX_CHARS);
        }
        ProjectEvents.notify(project, McpBuildLogListener.TOPIC,
                listener -> listener.logAppended(outputDirectory, line));
    }

    public @NotNull String snapshot(@NotNull String outputDirectory) {
        StringBuilder log = logs.get(outputDirectory);
        if (log == null) return "";
        synchronized (log) {
            return log.toString();
        }
    }

    public void clear(@NotNull String outputDirectory) {
        logs.remove(outputDirectory);
        ProjectEvents.notify(project, McpBuildLogListener.TOPIC,
                listener -> listener.logCleared(outputDirectory));
    }

    /** Drops the live buffer once the output has been persisted beside the generated server. */
    public void release(@NotNull String outputDirectory) {
        logs.remove(outputDirectory);
    }

    /**
     * Appends a completed operation to the durable server output. Deployment follows the original
     * server build in the same console, so users keep one chronological diagnostic record.
     */
    public void appendRecorded(@NotNull String outputDirectory, @NotNull CharSequence output) {
        if (output.isEmpty()) return;

        Path logFile = Path.of(outputDirectory).resolve(BUILD_LOG);
        try {
            Files.createDirectories(logFile.getParent());
            String separator = Files.isRegularFile(logFile) && Files.size(logFile) > 0 ? "\n" : "";
            Files.writeString(logFile, separator + output, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            conditionallyLog(e);
        }
    }
}
