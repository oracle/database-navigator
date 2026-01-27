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

package com.dbn.execution.java.wrapper.model;

import com.dbn.common.util.Strings;
import com.dbn.execution.java.wrapper.SqlType;
import com.dbn.execution.java.wrapper.TypeMappings;
import com.dbn.execution.java.wrapper.WrapperModel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NonNls;

@NonNls
@Getter
@Setter
public class ParameterWrapper extends EntityWrapper {
    private String javaTypeName;         // Java type name
    private String sqlTypeName;          // Java type name
    private String converterName;
    private int arrayDepth = 0;
    private boolean complexType;
    private boolean sqlConversionPossible;
    private String codeInput;

    public ParameterWrapper(WrapperModel model) {
        super(model);
    }

    public boolean isArray() {
        return arrayDepth > 0;
    }

    public String getSqlDeclarationSuffix() {
        if (arrayDepth > 0) return ""; // no declaration suffix for arrays. Only supported for scalars (e.g. VARCHAR2(3200))
        SqlType sqlType = TypeMappings.getSqlType(javaTypeName);
        return sqlType == null ? "" : sqlType.getDeclarationSuffix();
    }

    public boolean isCodeInput() {
        return Strings.isNotEmpty(codeInput);
    }
}
