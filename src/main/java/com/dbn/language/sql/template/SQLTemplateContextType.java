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

package com.dbn.language.sql.template;

import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.LeafPsiElement;
import com.dbn.language.common.psi.PsiUtil;
import com.dbn.language.psql.PSQLLanguage;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.vfs.file.DBDatasetFilterVirtualFile;
import com.dbn.vfs.file.DBObjectFilterExpressionFile;
import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SQLTemplateContextType extends TemplateContextType {
    protected SQLTemplateContextType() {
        super("SQL", "SQL (DBN)");
    }

    @Override
    public boolean isInContext(@NotNull PsiFile file, int offset) {
        Language language = file.getLanguage();
        if (language instanceof SQLLanguage) {
            VirtualFile virtualFile = file.getOriginalFile().getVirtualFile();
            if (virtualFile instanceof DBObjectFilterExpressionFile) return false;
            if (virtualFile instanceof DBDatasetFilterVirtualFile) return false;

            LeafPsiElement leafPsiElement = PsiUtil.lookupLeafBeforeOffset(file, offset);
            if (leafPsiElement == null) return true;

            if (leafPsiElement.getLanguage() instanceof PSQLLanguage) {
                BasePsiElement scopePsiElement = leafPsiElement.getEnclosingScopeElement();
                return scopePsiElement != null && !scopePsiElement.getTextRange().contains(offset);
            }

            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public SyntaxHighlighter createHighlighter() {
        return SQLLanguage.INSTANCE.getMainLanguageDialect().getSyntaxHighlighter();
    }
}
