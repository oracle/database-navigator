/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.assistant.tool;

import lombok.Getter;
import org.jetbrains.annotations.NonNls;

@NonNls
@Getter
public enum AssistantToolCategory {
    USER_INTERACTION("User interaction", "Interactive tools for quick user input"),
    CONFIG_INFO_PROVIDER("Config information provider", "Database configuration details"),
    METADATA_PROVIDER("Metadata provider", "Database structure insights"),
    SOURCE_CODE_PROVIDER("Source-code provider", "Source code for database objects"),
    DATA_PROVIDER("Data provider", "Actual data records and query results"),
    ACTION_INVOKER("Action invoker", "Database operations and external action invocations"),
    IDE_ACTION_INVOKER("IDE action invoker", "IDE action invocations, such as opening database object editors, executing queries, etc."),
    ;

    private final String description;
    private final String name;

    AssistantToolCategory(String name, String description) {
        this.name = name;
        this.description = description;
    }

}
