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

package com.dbn.code.common.lookup;

import com.dbn.code.common.completion.CodeCompletionContext;
import com.dbn.language.common.element.util.IdentifierType;
import com.dbn.language.common.psi.IdentifierPsiElement;
import com.dbn.object.type.DBObjectType;

import javax.swing.Icon;

public class IdentifierLookupItemBuilder extends LookupItemBuilder {
    private IdentifierPsiElement identifierPsiElement;
    public IdentifierLookupItemBuilder(IdentifierPsiElement identifierPsiElement) {
        this.identifierPsiElement = identifierPsiElement;
    }

    @Override
    public String getTextHint() {
        IdentifierType identifierType = identifierPsiElement.elementType.identifierType;
        DBObjectType objectType = identifierPsiElement.elementType.getObjectType();
        String objectTypeName = objectType == DBObjectType.ANY ? "object" : objectType.getName();
        String identifierTypeName =
                identifierType == IdentifierType.ALIAS  ? " alias" :
                identifierType == IdentifierType.VARIABLE ? " variable" :
                        "";
        return objectTypeName + identifierTypeName + (identifierPsiElement.isDefinition() ? " def" : " ref");
    }

    @Override
    public boolean isBold() {
        return false;
    }

    @Override
    public CharSequence getText(CodeCompletionContext completionContext) {
        return identifierPsiElement.getChars();
    }

    @Override
    public Icon getIcon() {
        DBObjectType objectType = identifierPsiElement.getObjectType();
        return objectType.getIcon();
    }
}