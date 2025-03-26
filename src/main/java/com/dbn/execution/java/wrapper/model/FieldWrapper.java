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

import com.dbn.execution.java.wrapper.SqlType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FieldWrapper {
    // Enum for access modifiers
    public enum AccessModifier {PUBLIC, PROTECTED, PRIVATE, DEFAULT}

    private String name;
    private String type;
    private boolean complexType = false;
    private int complexTypeIndexInWrapper = -1;
    private AccessModifier accessModifier = null;
    private String setter;
    private String getter;
    private short arrayDepth = 0;
    private short fieldIndex;
    private String sqlType;
    private String typeCastStart;
    private String typeCastEnd;

    public void setType(String type, SqlType sqlTypeDetails) {
        this.type = type;
        if (sqlTypeDetails != null) {
            sqlType = sqlTypeDetails.getSqlTypeName();
            typeCastStart = sqlTypeDetails.getTransformerPrefix();
            typeCastEnd = sqlTypeDetails.getTransformerSuffix();
        }
    }

    public boolean isArray() {
        return arrayDepth > 0;
    }

    public void setAccessModifier(String accessModifier) {
        if (accessModifier == null) {
            this.accessModifier = AccessModifier.DEFAULT;
            return;
        }

        switch (accessModifier.toLowerCase()) {
            case "public":
                this.accessModifier = AccessModifier.PUBLIC;
                break;
            case "protected":
                this.accessModifier = AccessModifier.PROTECTED;
                break;
            case "private":
                this.accessModifier = AccessModifier.PRIVATE;
                break;
            default:
                // Do nothing if the string doesn't match any valid modifier
                break;
        }
    }

}
