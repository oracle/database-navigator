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

package com.dbn.language.common.element.impl;

import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.parser.impl.OneOfElementTypeParser;
import com.dbn.language.common.element.parser.impl.SurrogateOneOfElementTypeParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SurrogateOneOfElementType extends OneOfElementType {

    private Map<TokenType, List<ElementTypeRef>> groupedChildren;

    public SurrogateOneOfElementType(ElementTypeBase parent, String id) {
        super(parent, id);
    }

    @Nullable
    public synchronized List<ElementTypeRef> getGroupedElements(TokenType tokenType) {
        if (groupedChildren == null) {
            initGroupedChildren();
        }
        return groupedChildren.get(tokenType);
    }

    private void initGroupedChildren() {
        groupedChildren = new LinkedHashMap<>();
        for (ElementTypeRef child : this.children) {
            LeafElementType leafElementType = (LeafElementType) child.elementType;
            TokenType tokenType = leafElementType.tokenType;
            List<ElementTypeRef> children = groupedChildren.computeIfAbsent(tokenType, k -> new ArrayList<>());
            children.add(child);
        }
    }

    @Override
    @NotNull
    protected OneOfElementTypeParser createParser() {
        return new SurrogateOneOfElementTypeParser(this);
    }


    @NotNull
    @Override
    public String getName() {
        return "one-of-tokens (" + getId() + ")";
    }
}
