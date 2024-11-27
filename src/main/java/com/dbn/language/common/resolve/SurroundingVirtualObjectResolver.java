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
import com.dbn.object.common.DBObject;
import com.dbn.object.type.DBObjectType;

public class SurroundingVirtualObjectResolver extends UnderlyingObjectResolver{
    private static final SurroundingVirtualObjectResolver INSTANCE = new SurroundingVirtualObjectResolver();

    public static SurroundingVirtualObjectResolver getInstance() {
        return INSTANCE;
    }

    private SurroundingVirtualObjectResolver() {
        super("VIRTUAL_OBJECT_RESOLVER");
    }

    @Override
    protected DBObject resolve(IdentifierPsiElement identifierPsiElement, int recursionCheck) {
        DBObjectType objectType = identifierPsiElement.getObjectType();
        if (objectType == DBObjectType.DATASET) return null;

        BasePsiElement virtualObjectPsiElement = identifierPsiElement.findEnclosingVirtualObjectElement(objectType);
        if (virtualObjectPsiElement == null) return null;

        return virtualObjectPsiElement.getUnderlyingObject();

    }
}
