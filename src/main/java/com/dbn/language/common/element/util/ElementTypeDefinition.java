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

package com.dbn.language.common.element.util;

import lombok.Getter;

import java.util.Objects;

@Getter
public enum ElementTypeDefinition {

    SEQUENCE("sequence"),
    ITERATION("iteration"),
    QUALIFIED_IDENTIFIER("qualified-identifier"),
    ONE_OF("one-of"),
    TOKEN("token"),
    ELEMENT("element"),
    WRAPPER("wrapper"),
    OBJECT_DEF("object-def"),
    OBJECT_REF("object-ref"),
    ALIAS_DEF("alias-def"),
    ALIAS_REF("alias-ref"),
    VARIABLE_DEF("variable-def"),
    VARIABLE_REF("variable-ref"),
    EXEC_VARIABLE("exec-variable"),
    BLOCK("block");

    ElementTypeDefinition(String name) {
        this.name = name;
    }

    private final String name;

    public boolean is(String name) {
        return Objects.equals(this.name, name);
    }
}

