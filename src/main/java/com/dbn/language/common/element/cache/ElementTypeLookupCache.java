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

package com.dbn.language.common.element.cache;

import com.dbn.common.index.IndexContainer;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.SharedTokenTypeBundle;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.TokenTypeCategory;
import com.dbn.language.common.element.ElementTypeBundle;
import com.dbn.language.common.element.impl.ElementTypeBase;
import com.dbn.language.common.element.impl.LeafElementType;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public interface ElementTypeLookupCache<T extends ElementTypeBase> {
    Set<TokenType> getNextPossibleTokens();

    boolean isNextPossibleToken(TokenType tokenType);

    DBLanguage getLanguage();

    ElementTypeBundle getElementTypeBundle();

    SharedTokenTypeBundle getSharedTokenTypes();

    boolean containsToken(TokenType tokenType);

    Set<TokenType> getAllPossibleTokens();

    Set<TokenType> getFirstPossibleTokens();

    Set<TokenType> getFirstRequiredTokens();

    boolean couldStartWithLeaf(LeafElementType elementType);

    boolean shouldStartWithLeaf(LeafElementType elementType);

    boolean couldStartWithToken(TokenType tokenType);

    Set<LeafElementType> getFirstPossibleLeafs();

    Set<LeafElementType> getFirstRequiredLeafs();

    boolean startsWith(TokenTypeCategory typeCategory);

    boolean isFirstPossibleToken(TokenType tokenType);

    boolean isFirstRequiredToken(TokenType tokenType);

    void captureFirstPossibleTokens(Set<TokenType> bucket);

    void captureFirstPossibleTokens(IndexContainer<TokenType> bucket);

    Set<LeafElementType> captureFirstPossibleLeafs(ElementLookupContext context);

    Set<TokenType> captureFirstPossibleTokens(ElementLookupContext context);

    Set<LeafElementType> captureFirstPossibleLeafs(ElementLookupContext context, @Nullable Set<LeafElementType> bucket);

    Set<TokenType> captureFirstPossibleTokens(ElementLookupContext context, @Nullable Set<TokenType> bucket);

    void registerLeaf(LeafElementType leaf, ElementTypeBase source);

}
