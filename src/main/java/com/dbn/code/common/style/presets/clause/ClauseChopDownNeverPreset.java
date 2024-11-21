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

package com.dbn.code.common.style.presets.clause;

import com.dbn.language.common.SharedTokenTypeBundle;
import com.dbn.language.common.TokenType;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.TokenPsiElement;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.Wrap;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.Nullable;

public class ClauseChopDownNeverPreset extends ClauseAbstractPreset {
    public ClauseChopDownNeverPreset() {
        super("do_not_chop_down", "Do not chop down");
    }

    @Override
    @Nullable
    public Wrap getWrap(BasePsiElement psiElement, CodeStyleSettings settings) {
        return WRAP_NONE;
    }

    @Override
    @Nullable
    public Spacing getSpacing(BasePsiElement psiElement, CodeStyleSettings settings) {
        PsiElement previousPsiElement = psiElement.getPrevSibling();
        if (previousPsiElement instanceof TokenPsiElement) {
            TokenPsiElement previousToken = (TokenPsiElement) previousPsiElement;
            SharedTokenTypeBundle sharedTokenTypes = psiElement.getLanguage().getSharedTokenTypes();
            TokenType tokenType = previousToken.getTokenType();
            return tokenType ==  sharedTokenTypes.getChrLeftParenthesis() ?
                    SPACING_NO_SPACE :
                    SPACING_ONE_SPACE;

        }
        return SPACING_ONE_SPACE;
    }
}
