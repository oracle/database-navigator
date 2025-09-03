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

@Getter
public enum AssistantToolCategory {
    METADATA_PROVIDER("Metadata provider", "Provides information about the database, such as schemas, tables, columns, etc."),
    SOURCE_CODE_PROVIDER("Source code provider", "Provides source code for various database objects, such as views, functions, procedures, etc."),
    DATA_PROVIDER("Data provider", "Delivers actual data content, such as records, rows, or query results."),
    ACTION_INVOKER("Action invoker", "Executes operations or tasks, such as running queries, applying updates, or triggering external actions."),
    ;

    private final String description;
    private final String name;

    AssistantToolCategory(String name, String description) {
        this.name = name;
        this.description = description;
    }

}
