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

package com.dbn.code.common.style.formatting;

import com.dbn.code.common.style.presets.CodeStylePreset;
import com.dbn.language.common.PsiElementRef;
import com.intellij.formatting.Alignment;
import com.intellij.formatting.Block;
import com.intellij.formatting.ChildAttributes;
import com.intellij.formatting.Indent;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.Wrap;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PassiveFormattingBlock implements Block {
    private static final  List<Block> EMPTY_LIST = new ArrayList<>(0);
    private final PsiElementRef<?> psiElement;

    public PassiveFormattingBlock(PsiElement psiElement) {
        this.psiElement = PsiElementRef.of(psiElement);
    }

    public PsiElement getPsiElement() {
        return PsiElementRef.get(psiElement);
    }

    @Override
    @NotNull
    public TextRange getTextRange() {
        return getPsiElement().getTextRange();
    }

    @Override
    @NotNull
    public List<Block> getSubBlocks() {
        return EMPTY_LIST;
    }

    @Override
    public Wrap getWrap() {
        return CodeStylePreset.WRAP_NONE;
    }

    @Override
    public Indent getIndent() {
        return Indent.getNoneIndent();
    }

    @Override
    public Alignment getAlignment() {
        return null;
    }

    @Override
    public Spacing getSpacing(@Nullable Block child1, @NotNull Block child2) {
        return null;
    }

    @Override
    @NotNull
    public ChildAttributes getChildAttributes(int newChildIndex) {
        return new ChildAttributes(Indent.getNoneIndent(), Alignment.createAlignment());
    }

    @Override
    public boolean isIncomplete() {
        return false;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }
}
