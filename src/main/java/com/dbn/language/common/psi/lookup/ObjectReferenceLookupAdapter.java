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

package com.dbn.language.common.psi.lookup;

import com.dbn.language.common.element.util.ElementTypeAttribute;
import com.dbn.language.common.element.util.IdentifierCategory;
import com.dbn.language.common.psi.LeafPsiElement;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;

public class ObjectReferenceLookupAdapter extends ObjectLookupAdapter {

    public ObjectReferenceLookupAdapter(LeafPsiElement lookupIssuer, DBObjectType objectType, CharSequence identifierName) {
        super(lookupIssuer, IdentifierCategory.REFERENCE, objectType, identifierName);
    }

    public ObjectReferenceLookupAdapter(LeafPsiElement lookupIssuer, @NotNull DBObjectType objectType, CharSequence identifierName, ElementTypeAttribute attribute) {
        super(lookupIssuer, IdentifierCategory.REFERENCE, objectType, identifierName, attribute);
    }
}
