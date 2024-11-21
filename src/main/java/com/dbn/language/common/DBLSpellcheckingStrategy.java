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

import com.dbn.editor.code.options.CodeEditorGeneralSettings;
import com.dbn.editor.code.options.CodeEditorSettings;
import com.dbn.language.common.psi.IdentifierPsiElement;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy;
import com.intellij.spellchecker.tokenizer.Tokenizer;
import org.jetbrains.annotations.NotNull;

public class DBLSpellcheckingStrategy extends SpellcheckingStrategy implements SpellcheckingSettingsListener {


    @NotNull
    @Override
    public Tokenizer getTokenizer(PsiElement element) {
        if (element instanceof PsiWhiteSpace) {
            return EMPTY_TOKENIZER;
        }

        CodeEditorSettings codeEditorSettings = CodeEditorSettings.getInstance(element.getProject());
        CodeEditorGeneralSettings codeEditorGeneralSettings = codeEditorSettings.getGeneralSettings();
        if (codeEditorGeneralSettings.isEnableSpellchecking()) {
            if (element instanceof PsiComment) {
                return TEXT_TOKENIZER;
            }

            if (element instanceof LeafPsiElement) {
                LeafPsiElement leafPsiElement = (LeafPsiElement) element;
                PsiElement parent = leafPsiElement.getParent();
                if (parent instanceof IdentifierPsiElement) {
                    IdentifierPsiElement identifierPsiElement = (IdentifierPsiElement) parent;
                    if (identifierPsiElement.isDefinition() || codeEditorGeneralSettings.isEnableReferenceSpellchecking()) {
                        return TEXT_TOKENIZER;
                    }
                }
            }
        }

        return EMPTY_TOKENIZER;
    }

    @Override
    public void settingsChanged() {

    }
}
