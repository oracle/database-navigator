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
import com.dbn.language.common.psi.lookup.ObjectReferenceLookupAdapter;
import com.dbn.language.common.psi.lookup.PsiLookupAdapter;
import com.dbn.object.common.DBObject;
import com.dbn.object.type.DBObjectType;

public class AssignmentObjectResolver extends UnderlyingObjectResolver{
    private static final AssignmentObjectResolver INSTANCE = new AssignmentObjectResolver();

    public static AssignmentObjectResolver getInstance() {
        return INSTANCE;
    }

    private AssignmentObjectResolver() {
        super("ASSIGNMENT_RESOLVER");
    }

    @Override
    protected DBObject resolve(IdentifierPsiElement identifierPsiElement, int recursionCheck) {
        NamedPsiElement enclosingNamedPsiElement = identifierPsiElement.findEnclosingNamedElement();
        if (enclosingNamedPsiElement == null) return null;

        PsiLookupAdapter lookupAdapter = new ObjectReferenceLookupAdapter(identifierPsiElement, DBObjectType.TYPE, null);
        BasePsiElement underlyingObjectCandidate = lookupAdapter.findInElement(enclosingNamedPsiElement);

        return underlyingObjectCandidate == null ? null : underlyingObjectCandidate.getUnderlyingObject() ;
    }
}
