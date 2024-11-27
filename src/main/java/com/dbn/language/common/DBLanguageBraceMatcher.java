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

package com.dbn.language.common;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public abstract class DBLanguageBraceMatcher implements PairedBraceMatcher {
    private final BracePair[] bracePairs;
    private final DBLanguage language;

    public DBLanguageBraceMatcher(DBLanguage language) {
        this.language = language;
        SharedTokenTypeBundle tt = language.getSharedTokenTypes();
        bracePairs = new BracePair[]{
            new BracePair(tt.getChrLeftParenthesis(), tt.getChrRightParenthesis(), false),
            new BracePair(tt.getChrLeftBracket(), tt.getChrRightBracket(), false)};
    }

    @NotNull
    @Override
    public BracePair[] getPairs() {
        return bracePairs;
    }

    @Override
    public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType iElementType, @Nullable IElementType iElementType1) {
        if (iElementType1 instanceof SimpleTokenType) {
            SimpleTokenType simpleTokenType = (SimpleTokenType) iElementType1;
            SharedTokenTypeBundle tt = language.getSharedTokenTypes();
            return simpleTokenType == tt.getWhiteSpace() ||
                    simpleTokenType == tt.getChrDot() ||
                    simpleTokenType == tt.getChrComma() ||
                    simpleTokenType == tt.getChrColon() ||
                    simpleTokenType == tt.getChrSemicolon();

        }
        return iElementType1 == null;
    }

    @Override
    public int getCodeConstructStart(PsiFile psiFile, int i) {
        return i;
    }
}
