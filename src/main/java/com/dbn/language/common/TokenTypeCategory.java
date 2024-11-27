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

package com.dbn.language.common;

import lombok.Getter;

import java.util.Objects;

@Getter
public enum TokenTypeCategory {
    UNKNOWN("unknown"),
    KEYWORD("keyword"),
    FUNCTION("function"),
    PARAMETER("parameter"),
    DATATYPE("datatype"),
    OBJECT("object"),
    EXCEPTION("exception"),
    OPERATOR("operator"),
    CHARACTER("character"),
    IDENTIFIER("identifier"),
    CHAMELEON("chameleon"),
    WHITESPACE("whitespace"),
    COMMENT("comment"),
    NUMERIC("numeric"),
    LITERAL("literal")
    ;

    private final String name;

    TokenTypeCategory(String name) {
        this.name = name;
    }

    public static TokenTypeCategory getCategory(String categoryName) {
        for (TokenTypeCategory identifier : TokenTypeCategory.values()) {
            if (Objects.equals(identifier.name, categoryName)) return identifier;
        }
        return UNKNOWN;
    }
}
