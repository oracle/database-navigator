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

package com.dbn.assistant;

import lombok.Getter;

import static com.dbn.nls.NlsResources.txt;

/**
 * The type of backend behind the database assistant
 *  - GENERIC - custom AI implementation (direct interface to AI provider pre-prompting with the data model as ddl statements) TBD
 *  - ORACLE_AI - Oracle Autonomous Database Select AI
 *
 * @author Dan Cioca (Oracle)
 */
@Getter
public enum AssistantType {
    PUBLIC    (txt("app.assistant.title.DatabaseAssistantName_PUBLIC")),         // external publicly available language model
    LOCAL     (txt("app.assistant.title.DatabaseAssistantName_LOCAL")),          // local llm (internally deployed llm)
    CUSTOM    (txt("app.assistant.title.DatabaseAssistantName_CUSTOM")),   // custom llm (user defined llm)
    SELECT_AI (txt("app.assistant.title.DatabaseAssistantName_SELECT_AI")),   // oracle select ai
    VECTOR_AI (txt("app.assistant.title.DatabaseAssistantName_VECTOR_AI"))  // oracle vector-ai search
    ;

    private final String name;

    AssistantType(String name) {
        this.name = name;
    }
}
