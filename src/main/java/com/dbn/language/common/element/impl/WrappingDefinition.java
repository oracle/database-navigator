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

import com.dbn.language.common.element.TokenPairTemplate;

import java.util.Objects;

public class WrappingDefinition {
    public final TokenElementType beginElement;
    public final TokenElementType endElement;
    public final boolean optional;
    public final TokenPairTemplate template;

    public WrappingDefinition(TokenElementType beginElement, TokenElementType endElement, boolean optional) {
        this.beginElement = beginElement;
        this.endElement = endElement;
        this.optional = optional;
        this.template = beginElement.tokenType.getTokenPairTemplate();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (getClass() != o.getClass()) return false;

        WrappingDefinition that = (WrappingDefinition) o;
        return this.optional == that.optional &&
                this.beginElement.tokenType == that.beginElement.tokenType &&
                this.endElement.tokenType == that.endElement.tokenType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                optional,
                beginElement.tokenType,
                endElement.tokenType);
    }
}
