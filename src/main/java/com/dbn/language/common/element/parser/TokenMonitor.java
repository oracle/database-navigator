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

package com.dbn.language.common.element.parser;

import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.LeafElementType;
import com.intellij.util.containers.Stack;

import java.util.LinkedHashSet;
import java.util.Set;

public class TokenMonitor extends ParserBuilderExtension {
    private final Stack<SurrogateMarker> surrogateStack = new Stack<>();
    public LeafElementType lastLeaf;

    protected TokenMonitor(ParserBuilder builder) {
        super(builder);
    }

    public void markResolved(LeafElementType leaf) {
        lastLeaf = leaf;
    }

    public void enterSurrogateSection(LeafElementType surrogateLeaf) {
        SurrogateMarker surrogateMarker = new SurrogateMarker(surrogateLeaf, builder.getOffset());
        surrogateStack.push(surrogateMarker);
    }

    public void exitSurrogateSection(LeafElementType surrogateLeaf) {
        SurrogateMarker surrogateMarker = surrogateStack.pop();
        assert surrogateMarker.elementType == surrogateLeaf;
    }

    public boolean isSurrogate() {
        if (surrogateStack.isEmpty()) return false;

        SurrogateMarker surrogateMarker = surrogateStack.peek();
        if (surrogateMarker.builderOffset != builder.getOffset()) return false;
        return true;
    }

    public boolean isSurrogateConsumed() {
        if (lastLeaf == null) return false;

/*        if (!surrogateStack.isEmpty()) {
            SurrogateMarker lastSurrogate = surrogateStack.peek();
            if (lastSurrogate.elementType.surrogateFor.contains(lastLeaf)) return true;
        }*/

        if (builder.tokenPairMonitor.isConsumedMatch(lastLeaf.tokenType)) return true;

        return false;
    }

    public boolean isSurrogateFor(ElementTypeBase elementType) {
        if (surrogateStack.isEmpty()) return false;

        SurrogateMarker surrogateMarker = surrogateStack.peek();
        if (surrogateMarker.builderOffset != builder.getOffset()) return false;

        return surrogateMarker.elementType.isSurrogateFor(elementType);
    }


    public LeafElementType getLastSurrogate() {
        return surrogateStack.isEmpty() ? null : surrogateStack.peek().elementType;
    }

/*    public boolean isStartSurrogateFor(ElementTypeBase elementType) {
        if (surrogateStack.isEmpty()) return false;
        return lastSurrogate.startSurrogateFor.contains(elementType);
    }*/

    public static Set<LeafElementType> unwrapSurrogates(Set<LeafElementType> leafs) {
        Set<LeafElementType> collector = new LinkedHashSet<>();
        for (LeafElementType leaf : leafs) {
            unwrapSurrogate(leaf, collector);
        }
        return collector;
    }

    private static void unwrapSurrogate(LeafElementType leafElementType, Set<LeafElementType> collector) {
        Set<LeafElementType> surrogateFor = leafElementType.surrogateFor;
        if (surrogateFor == null) {
            collector.add(leafElementType);
            return;
        }

        for (LeafElementType elementType : surrogateFor) {
            unwrapSurrogate(elementType, collector);
        }
    }

    private record SurrogateMarker(LeafElementType elementType, int builderOffset) {
    }
}
