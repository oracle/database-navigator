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

package com.dbn.execution.java.wrapper;

import lombok.Getter;
import org.jetbrains.annotations.NonNls;

@NonNls
@Getter
public class SqlType {
    private final String sqlTypeName;
    private final String transformerPrefix;
    private final String transformerSuffix;
    private final String declarationSuffix;

    public SqlType(String sqlTypeName) {
        this(sqlTypeName, "", "", "");
    }
    public SqlType(String sqlTypeName, String transformerPrefix, String transformerSuffix) {
        this(sqlTypeName, transformerPrefix, transformerSuffix, "");
    }

    public SqlType(String sqlTypeName, String transformerPrefix, String transformerSuffix, String declarationSuffix) {
        this.sqlTypeName = sqlTypeName;
        this.transformerPrefix = transformerPrefix;
        this.transformerSuffix = transformerSuffix;
        this.declarationSuffix = declarationSuffix;
    }

}