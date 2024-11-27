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

import com.dbn.language.common.SharedTokenTypeBundle;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.element.impl.ExecVariableElementType;

import java.util.HashSet;
import java.util.Set;

public class ExecVariableElementTypeLookupCache extends LeafElementTypeLookupCache<ExecVariableElementType>{
    public ExecVariableElementTypeLookupCache(ExecVariableElementType elementType) {
        super(elementType);
    }

    @Override
    public boolean isFirstPossibleToken(TokenType tokenType) {
        return tokenType.isVariable();
    }

    @Override
    public boolean isFirstRequiredToken(TokenType tokenType) {
        return tokenType.isVariable();
    }


    @Override
    public Set<TokenType> getFirstPossibleTokens() {
        Set<TokenType> firstPossibleTokens = new HashSet<>(1);
        SharedTokenTypeBundle sharedTokenTypes = getSharedTokenTypes();
        TokenType variable = sharedTokenTypes.getVariable();
        firstPossibleTokens.add(variable);
        return firstPossibleTokens;
    }

    @Override
    public void captureFirstPossibleTokens(Set<TokenType> bucket) {
        bucket.add(getSharedTokenTypes().getVariable());
    }

    @Override
    public boolean containsToken(TokenType tokenType) {
        SharedTokenTypeBundle sharedTokenTypes = getSharedTokenTypes();
        return sharedTokenTypes.getVariable() == tokenType;
    }

    @Override
    public boolean startsWithIdentifier() {
        return false;
    }
}
