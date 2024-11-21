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

import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.language.common.psi.lookup.ObjectDefinitionLookupAdapter;
import com.dbn.language.common.psi.lookup.PsiLookupAdapter;
import com.dbn.object.type.DBObjectType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;

public class PSQLFile extends DBLanguagePsiFile {

    PSQLFile(FileViewProvider fileViewProvider) {
        super(fileViewProvider, PSQLFileType.INSTANCE, PSQLLanguage.INSTANCE);
    }

    public BasePsiElement lookupObjectSpecification(DBObjectType objectType, CharSequence objectName) {
        PsiLookupAdapter lookupAdapter = new ObjectDefinitionLookupAdapter(null, objectType, objectName, ElementTypeAttribute.SUBJECT);
        PsiElement child = getFirstChild();
        while (child != null) {
            if (child instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) child;
                BasePsiElement specObject = lookupAdapter.findInScope(basePsiElement);
                if (specObject != null) {
                    return specObject.findEnclosingElement(ElementTypeAttribute.OBJECT_SPECIFICATION);
                }
            }
            child = child.getNextSibling();
        }
        return null;
    }

    public BasePsiElement lookupObjectDeclaration(DBObjectType objectType, CharSequence objectName) {
        PsiLookupAdapter lookupAdapter = new ObjectDefinitionLookupAdapter(null, objectType, objectName, ElementTypeAttribute.SUBJECT);
        PsiElement child = getFirstChild();
        while (child != null) {
            if (child instanceof BasePsiElement) {
                BasePsiElement basePsiElement = (BasePsiElement) child;
                BasePsiElement specObject = lookupAdapter.findInScope(basePsiElement);
                if (specObject != null) {
                    return specObject.findEnclosingElement(ElementTypeAttribute.OBJECT_DECLARATION);
                }
            }
            child = child.getNextSibling();
        }
        return null;
    }
}
