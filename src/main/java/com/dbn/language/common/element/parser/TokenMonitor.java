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

public class TokenMonitor extends ParserBuilderExtension {
    public LeafElementType lastLeaf;
    public int lastLeafOffset;

    protected TokenMonitor(ParserBuilder builder) {
        super(builder);
    }

    public void markResolved(LeafElementType leaf) {
        if (sameBuilderOffset() && leaf.isSurrogate()) return;

        lastLeaf = leaf;
        lastLeafOffset = builder.getOffset();
    }

    private boolean sameBuilderOffset() {
        return lastLeafOffset == builder.getOffset();
    }

    public boolean isSurrogate() {
        if (lastLeaf == null) return false;
        if (lastLeaf.surrogateFor == null) return false;
        if (sameBuilderOffset()) return true;

        return false;
    }

    public boolean isSurrogateFor(LeafElementType leafElementType) {
        if (!isSurrogate()) return false;
        return lastLeaf.surrogateFor.contains(leafElementType);
    }

    public boolean isStartSurrogateFor(ElementTypeBase elementType) {
        if (!isSurrogate()) return false;
        return lastLeaf.startSurrogateFor.contains(elementType);
    }
}
