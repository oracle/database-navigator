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

package com.dbn.language.common.element;

import lombok.Getter;

import java.util.Objects;

@Getter
public enum TokenPairTemplate {
    PARENTHESES("CHR_LEFT_PARENTHESIS", "CHR_RIGHT_PARENTHESIS", false),
    BRACKETS("CHR_LEFT_BRACKET", "CHR_RIGHT_BRACKET", false),
    BEGIN_END("KW_BEGIN", "KW_END", true);

    private final String beginToken;
    private final String endToken;
    private final boolean block;

    TokenPairTemplate(String beginToken, String endToken, boolean block) {
        this.beginToken = beginToken;
        this.endToken = endToken;
        this.block = block;
    }

    public static TokenPairTemplate get(String tokenTypeId) {
        for (TokenPairTemplate tokenPairTemplate : values()) {
            if (Objects.equals(tokenPairTemplate.beginToken, tokenTypeId) ||
                    Objects.equals(tokenPairTemplate.endToken, tokenTypeId)) {
                return tokenPairTemplate;
            }
        }
        return null;
    }
}
