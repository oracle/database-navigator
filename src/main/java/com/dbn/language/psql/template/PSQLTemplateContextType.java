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

package com.dbn.language.psql.template;

import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.LeafPsiElement;
import com.dbn.language.common.psi.PsiUtil;
import com.dbn.language.psql.PSQLLanguage;
import com.dbn.language.sql.SQLLanguage;
import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PSQLTemplateContextType extends TemplateContextType {
    protected PSQLTemplateContextType() {
        super("PL/SQL", "PL/SQL (DBN)");
    }

    @Override
    public boolean isInContext(@NotNull PsiFile file, int offset) {
        Language language = file.getLanguage();
        if (language instanceof SQLLanguage) {
            // support PSQL in SQL language
            LeafPsiElement leafPsiElement = PsiUtil.lookupLeafBeforeOffset(file, offset);
            if (leafPsiElement != null) {
                if (leafPsiElement.getLanguage() instanceof PSQLLanguage) {
                    BasePsiElement scopePsiElement = leafPsiElement.getEnclosingScopeElement();
                    return scopePsiElement != null && scopePsiElement.getTextRange().contains(offset);
                }
            }
        }
        return language instanceof PSQLLanguage;
    }

    @Nullable
    @Override
    public SyntaxHighlighter createHighlighter() {
        return PSQLLanguage.INSTANCE.getMainLanguageDialect().getSyntaxHighlighter();
    }
}
