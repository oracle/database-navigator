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

package com.dbn.assistant.tool;

import com.dbn.assistant.AssistantMode;
import com.dbn.common.constant.Constant;

import java.util.Map;
import java.util.Set;

public enum AssistantToolType implements Constant<AssistantToolType> {
    EXTERNAL,

    // USER_INTERACTION
    USER_PROMPTS,

    // DATA_PROVIDER
    SEMANTIC_SEARCH,

    // METADATA_PROVIDER
    DATABASE_METADATA,
    SCHEMA_METADATA,
    TABLE_METADATA,
    VIEW_METADATA,
    PROGRAM_METADATA,
    JAVA_METADATA,

    // SOURCE_CODE_PROVIDER
    VIEW_SOURCE_CODE,
    PROGRAM_SOURCE_CODE,
    JAVA_SOURCE_CODE,


    // IDE_ACTION_INVOKER
    DATASET_EDITORS,
    SQL_CONSOLE_EDITORS,
    PROGRAM_SOURCE_CODE_EDITORS,
    JAVA_SOURCE_CODE_EDITORS,

    ;

    public static final Map<AssistantMode, Set<AssistantToolType>> SUPPORT = Map.of(
            // code development
            AssistantMode.DEVELOPMENT, Set.of(
                USER_PROMPTS,
                DATABASE_METADATA,
                SCHEMA_METADATA,
                TABLE_METADATA,
                VIEW_METADATA,
                PROGRAM_METADATA,
                JAVA_METADATA,
                VIEW_SOURCE_CODE,
                PROGRAM_SOURCE_CODE,
                JAVA_SOURCE_CODE,
                DATASET_EDITORS,
                PROGRAM_SOURCE_CODE_EDITORS,
                JAVA_SOURCE_CODE_EDITORS,
                SQL_CONSOLE_EDITORS),

            // data analytics
            AssistantMode.ANALYTICS, Set.of(
                USER_PROMPTS,
                DATABASE_METADATA,
                SCHEMA_METADATA,
                TABLE_METADATA,
                VIEW_METADATA,
                VIEW_SOURCE_CODE,
                DATASET_EDITORS,
                SQL_CONSOLE_EDITORS),

            // semantic search
            AssistantMode.RAG, Set.of(
                USER_PROMPTS,
                SEMANTIC_SEARCH));

}
