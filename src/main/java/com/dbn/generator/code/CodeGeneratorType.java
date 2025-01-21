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

package com.dbn.generator.code;

import lombok.Getter;
import org.jetbrains.annotations.NonNls;

import static com.dbn.generator.code.CodeGeneratorCategory.JAVA;

@NonNls
@Getter
public enum CodeGeneratorType {
    DATABASE_CONNECTOR(              JAVA, "JdbcConnector", "JDBC Connector",                   "DBN - JDBC Connector.java"),
    DATA_SELECTOR(                   JAVA, "", "", ""),
    METHOD_EXECUTOR(                 JAVA, "", "", ""),
    //...
    ;

    private final CodeGeneratorCategory category;
    private final String name;
    private final String template;
    private final String fileName;

    CodeGeneratorType(CodeGeneratorCategory category, String fileName, String name, @NonNls String template) {
        this.category = category;
        this.fileName = fileName;
        this.name = name;
        this.template = template;
    }
}
