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

import com.dbn.common.constant.Constant;

public enum AssistantToolType implements Constant<AssistantToolType> {
    USER_PROMPTS,
    SEMANTIC_SEARCH,
    CONNECTION_INFO,
    DATABASE_METADATA,

    SCHEMA_METADATA,
    TABLE_METADATA,
    VIEW_METADATA,
    PROGRAM_METADATA,

    VIEW_SOURCE_CODE,
    PROGRAM_SOURCE_CODE,

    DATASET_EDITORS,
    SOURCE_CODE_EDITORS,

    SQL_CONSOLE_EDITORS,

    ;

/*
    public static class Converter extends PseudoConstantConverter<AssistantToolType> {
        public Converter() {
            super(AssistantToolType.class);
        }
    }
*/
}
