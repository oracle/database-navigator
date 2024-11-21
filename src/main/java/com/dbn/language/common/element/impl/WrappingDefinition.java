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

package com.dbn.language.common.element.impl;

import java.util.Objects;

public class WrappingDefinition {
    public final TokenElementType beginElementType;
    public final TokenElementType endElementType;

    public WrappingDefinition(TokenElementType beginElementType, TokenElementType endElementType) {
        this.beginElementType = beginElementType;
        this.endElementType = endElementType;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if (obj instanceof WrappingDefinition) {
            WrappingDefinition definition = (WrappingDefinition) obj;
            return
                Objects.equals(this.beginElementType.tokenType, definition.beginElementType.tokenType) &&
                Objects.equals(this.endElementType.tokenType, definition.endElementType.tokenType);
        }
        return false;
    }
}
