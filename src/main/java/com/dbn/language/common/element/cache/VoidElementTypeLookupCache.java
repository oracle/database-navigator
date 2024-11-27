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

package com.dbn.language.common.element.cache;

import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.LeafElementType;

import java.util.Set;

public class VoidElementTypeLookupCache<T extends ElementTypeBase> extends ElementTypeLookupCache<T>{
    public VoidElementTypeLookupCache(T elementType) {
        super(elementType);
    }

    @Override
    public boolean containsToken(TokenType tokenType) {
        return false;
    }

    @Override
    public boolean containsLeaf(LeafElementType elementType) {
        return false;
    }

    @Override
    public Set<TokenType> getFirstPossibleTokens() {
        return null;
    }

    @Override
    public Set<TokenType> getFirstRequiredTokens() {
        return null;
    }

    @Override
    public boolean couldStartWithLeaf(LeafElementType elementType) {
        return false;
    }

    @Override
    public boolean shouldStartWithLeaf(LeafElementType elementType) {
        return false;
    }

    @Override
    public boolean couldStartWithToken(TokenType tokenType) {
        return false;
    }

    @Override
    public Set<LeafElementType> getFirstPossibleLeafs() {
        return null;
    }

    @Override
    public Set<LeafElementType> getFirstRequiredLeafs() {
        return null;
    }

    @Override
    public boolean isFirstPossibleLeaf(LeafElementType elementType) {
        return false;
    }

    @Override
    public boolean isFirstRequiredLeaf(LeafElementType elementType) {
        return false;
    }

    @Override
    public boolean startsWithIdentifier() {
        return false;
    }

    @Override
    public boolean isFirstPossibleToken(TokenType tokenType) {
        return false;
    }

    @Override
    public boolean isFirstRequiredToken(TokenType tokenType) {
        return false;
    }
}
