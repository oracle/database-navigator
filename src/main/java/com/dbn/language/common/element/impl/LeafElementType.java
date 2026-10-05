/*
 * Copyright 2026 Oracle and/or its affiliates
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

import com.dbn.common.index.Indexable;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.util.ElementTypeDefinitionException;
import org.jdom.Element;

import java.util.Set;

public abstract class LeafElementType extends ElementTypeBase implements Indexable {
    public TokenType tokenType;

    public boolean optional;
    private final int idx;

    LeafElementType(ElementTypeBundle bundle, ElementTypeBase parent, String id, Element def) throws ElementTypeDefinitionException {
        super(bundle, parent, id, def);
        idx = bundle.nextIndex();
        bundle.registerElement(this);
    }

    LeafElementType(ElementTypeBundle bundle, ElementTypeBase parent, String id) {
        super(bundle, parent, id);
        idx = bundle.nextIndex();
        bundle.registerElement(this);
    }

    @Override
    public int index() {
        return idx;
    }

    public void registerLeaf() {
        parent.cache.registerLeaf(this, this);
    }

    @Override
    public TokenType getTokenType() {
        return tokenType;
    }


    public abstract boolean isSameAs(LeafElementType elementType);

    public abstract boolean isIdentifier();

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public void collectAnonymousLeafs(Set<LeafElementType> bucket) {
        super.collectAnonymousLeafs(bucket);
        bucket.add(this);
    }
}
