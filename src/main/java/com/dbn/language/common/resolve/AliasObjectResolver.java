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
import com.dbn.language.common.psi.PsiUtil;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectPsiElement;
import com.intellij.psi.PsiElement;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

@Slf4j
public class AliasObjectResolver extends UnderlyingObjectResolver{

    private static final AliasObjectResolver INSTANCE = new AliasObjectResolver();

    public static AliasObjectResolver getInstance() {
        return INSTANCE;
    }

    private AliasObjectResolver() {
        super("ALIAS_RESOLVER");
    }

    @Override
    @Nullable
    protected DBObject resolve(IdentifierPsiElement identifierPsiElement, int recursionCheck) {
        if (recursionCheck > 10) return null;

        BasePsiElement aliasedObject = PsiUtil.resolveAliasedEntityElement(identifierPsiElement);
        if (aliasedObject == null) return null;

        if (aliasedObject.isVirtualObject()) {
            return aliasedObject.getUnderlyingObject();

        } else if (aliasedObject instanceof IdentifierPsiElement) {
            IdentifierPsiElement aliasedPsiElement = (IdentifierPsiElement) aliasedObject;
            PsiElement underlyingPsiElement = aliasedPsiElement.resolve();
            if (underlyingPsiElement instanceof DBObjectPsiElement) {
                DBObjectPsiElement objectPsiElement = (DBObjectPsiElement) underlyingPsiElement;
                return objectPsiElement.ensureObject();
            }

            if (underlyingPsiElement instanceof IdentifierPsiElement && underlyingPsiElement != identifierPsiElement) {
                IdentifierPsiElement underlyingIdentifierPsiElement = (IdentifierPsiElement) underlyingPsiElement;
                if (underlyingIdentifierPsiElement.isAlias() && underlyingIdentifierPsiElement.isDefinition()) {
                    recursionCheck++;
                    return resolve(underlyingIdentifierPsiElement, recursionCheck);
                }
            }
        }
        return null;
    }
}
