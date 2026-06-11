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

package com.dbn.language.psql;

import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.IdentifierPsiElement;
import com.dbn.language.common.psi.PsiUtil;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectPsiElement;
import com.dbn.object.navigation.DBObjectNavigationInfoProvider;
import com.dbn.object.navigation.DBObjectNavigationInfoProviderCache;
import com.dbn.object.type.DBObjectType;
import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.dbn.nls.NlsResources.txt;

public class PSQLDocumentationProvider implements DocumentationProvider {

    @Nullable
    private String getQuickNavigateInfo(PsiElement element) {
        if (element instanceof DBObjectPsiElement objectPsiElement) {
            DBObject object = objectPsiElement.ensureObject();
            DBObjectType objectType = object.getObjectType();
            DBObjectNavigationInfoProvider<DBObject> infoProvider = DBObjectNavigationInfoProviderCache.get(objectType);
            if (infoProvider == null) return null;

            return infoProvider.getNavigationTooltipText(object);
        }

        if (element instanceof IdentifierPsiElement identifierPsiElement) {
            String objectTypeName = identifierPsiElement.getObjectType().getName();

            // ALIAS
            if (identifierPsiElement.isAlias()) {
                if (!identifierPsiElement.isDefinition()) return null;

                BasePsiElement aliasedObjectElement = PsiUtil.resolveAliasedEntityElement(identifierPsiElement);
                if (aliasedObjectElement == null) {
                    return txt("app.codeEditor.text.UnknownAlias");
                }

                DBObject aliasedObject = aliasedObjectElement.getUnderlyingObject();
                if (aliasedObject == null) {
                    return txt("app.codeEditor.text.AliasOf", aliasedObjectElement.getReferenceQualifiedName());
                }
                return txt("app.codeEditor.text.AliasOf", aliasedObject.getQualifiedNameWithType());
            }

            // OBJECT
            if (identifierPsiElement.isObject()) {
                if (!identifierPsiElement.isDefinition()) return null;

                BasePsiElement contextPsiElement = identifierPsiElement.findEnclosingVirtualObjectElement(identifierPsiElement.getObjectType());
                if (contextPsiElement == null) {
                    contextPsiElement = identifierPsiElement.findEnclosingNamedElement();
                }
                return contextPsiElement == null ? objectTypeName : txt("app.codeEditor.text.QuickInfoDetails", objectTypeName, contextPsiElement.getText());
            }

            // VARIABLE
            if (identifierPsiElement.isVariable()) {
                BasePsiElement contextPsiElement = identifierPsiElement.findEnclosingVirtualObjectElement(identifierPsiElement.getObjectType());
                if (contextPsiElement == null) {
                    contextPsiElement = identifierPsiElement.findEnclosingNamedElement();
                }

                String prefix = identifierPsiElement.getObjectType() == DBObjectType.ANY ? txt("app.codeEditor.text.Variable") : objectTypeName;
                return contextPsiElement == null ? prefix : txt("app.codeEditor.text.QuickInfoIndentedDetails", prefix, contextPsiElement.getText());
           }
        }
        return null;
    }

    @Override
    public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        return getQuickNavigateInfo(element);
    }

    @Override
    @Nullable
    public List<String> getUrlFor(PsiElement psiElement, PsiElement psiElement1) {
        return null;
    }

    @Override
    @Nullable
    public String generateDoc(PsiElement psiElement, PsiElement psiElement1) {
        return null;
    }

    @Override
    @Nullable
    public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object o, PsiElement psiElement) {
        return null;
    }

    @Override
    @Nullable
    public PsiElement getDocumentationElementForLink(PsiManager psiManager, String s, PsiElement psiElement) {
        return null;
    }
}
