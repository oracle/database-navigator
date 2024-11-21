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

package com.dbn.language.common.resolve;

import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.IdentifierPsiElement;
import com.dbn.language.common.psi.NamedPsiElement;
import com.dbn.language.common.psi.lookup.IdentifierLookupAdapter;
import com.dbn.language.common.psi.lookup.PsiLookupAdapter;
import com.dbn.object.common.DBObject;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;

public class LocalDeclarationObjectResolver extends UnderlyingObjectResolver{
    private static final LocalDeclarationObjectResolver INSTANCE = new LocalDeclarationObjectResolver();

    public static LocalDeclarationObjectResolver getInstance() {
        return INSTANCE;
    }

    private LocalDeclarationObjectResolver() {
        super("LOCAL_DECLARATION_RESOLVER");
    }

    @Override
    protected DBObject resolve(IdentifierPsiElement identifierPsiElement, int recursionCheck) {

        NamedPsiElement enclosingNamedPsiElement = identifierPsiElement.findEnclosingNamedElement();
        if (enclosingNamedPsiElement == null) return null;

        BasePsiElement underlyingObjectCandidate;
        DBObjectType objectType = identifierPsiElement.getObjectType();
        if (objectType.matches(DBObjectType.DATASET)) {
            underlyingObjectCandidate = findObject(identifierPsiElement, enclosingNamedPsiElement, DBObjectType.DATASET);

        } else if (objectType.matches(DBObjectType.TYPE)) {
            underlyingObjectCandidate = findObject(identifierPsiElement, enclosingNamedPsiElement, DBObjectType.TYPE);

        } else if (objectType == DBObjectType.ANY || objectType == DBObjectType.ARGUMENT) {
            underlyingObjectCandidate = findObject(identifierPsiElement, enclosingNamedPsiElement, DBObjectType.TYPE);
            if (underlyingObjectCandidate == null) {
                underlyingObjectCandidate = findObject(identifierPsiElement, enclosingNamedPsiElement, DBObjectType.DATASET);
            }
        } else {
            underlyingObjectCandidate = findObject(identifierPsiElement, enclosingNamedPsiElement, objectType);
        }

        return underlyingObjectCandidate == null ? null : underlyingObjectCandidate.getUnderlyingObject() ;
    }

    private static BasePsiElement findObject(IdentifierPsiElement identifierPsiElement, @NotNull NamedPsiElement enclosingNamedPsiElement, DBObjectType objectType) {
        PsiLookupAdapter lookupAdapter = new IdentifierLookupAdapter(identifierPsiElement, null, null, objectType, null);
        return lookupAdapter.findInElement(enclosingNamedPsiElement);
    }
}
