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

package com.dbn.language.common.psi.lookup;

import com.dbn.language.common.TokenType;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.TokenPsiElement;

import java.util.function.Function;

public class TokenTypeLookupAdapter extends PsiLookupAdapter{
    private final Function<BasePsiElement, TokenType> tokenType;

    public TokenTypeLookupAdapter(Function<BasePsiElement, TokenType> tokenType) {
        this.tokenType = tokenType;
    }

    @Override
    public boolean matches(BasePsiElement element) {
        if (element instanceof TokenPsiElement) {
            TokenPsiElement tokenPsiElement = (TokenPsiElement) element;
            return tokenPsiElement.getTokenType() == tokenType.apply(element);
        }
        return false;
    }

    @Override
    public boolean accepts(BasePsiElement element) {
        return true;
    }


}
