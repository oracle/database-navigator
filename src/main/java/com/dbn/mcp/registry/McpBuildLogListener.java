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

import com.intellij.util.messages.Topic;

import java.util.EventListener;

/**
 * Build output as it is produced. Kept separate from the registry events because a log line must
 * not rebuild the server list - it only appends to whichever output view is showing that server.
 */
public interface McpBuildLogListener extends EventListener {
    Topic<McpBuildLogListener> TOPIC = Topic.create("MCP server build log event", McpBuildLogListener.class);

    /** @param outputDirectory identifies the server the line belongs to */
    default void logAppended(String outputDirectory, String line) {}

    default void logCleared(String outputDirectory) {}
}
