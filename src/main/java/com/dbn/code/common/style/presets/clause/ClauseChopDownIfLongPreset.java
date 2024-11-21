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

import com.dbn.language.common.psi.BasePsiElement;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.Wrap;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.Nullable;

public class ClauseChopDownIfLongPreset extends ClauseAbstractPreset {
    public ClauseChopDownIfLongPreset() {
        super("chop_down_if_long", "Chop down if long");
    }

    @Override
    @Nullable
    public Wrap getWrap(BasePsiElement psiElement, CodeStyleSettings settings) {
        boolean shouldWrap = psiElement.approximateLength() > settings.getRightMargin(psiElement.getLanguage());
        return shouldWrap ? WRAP_ALWAYS : WRAP_NONE;

    }

    @Override
    @Nullable
    public Spacing getSpacing(BasePsiElement psiElement, CodeStyleSettings settings) {
        boolean shouldChopDown = psiElement.approximateLength() > settings.getRightMargin(psiElement.getLanguage());
        return getSpacing(psiElement, shouldChopDown);
    }
}