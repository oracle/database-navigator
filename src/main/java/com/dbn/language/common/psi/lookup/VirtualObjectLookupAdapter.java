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

import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.object.type.DBObjectType;

public class VirtualObjectLookupAdapter extends PsiLookupAdapter {
    private final DBObjectType parentObjectType;
    private final DBObjectType objectType;

    public VirtualObjectLookupAdapter(DBObjectType parentObjectType, DBObjectType objectType) {
        this.parentObjectType = parentObjectType;
        this.objectType = objectType;
    }

    @Override
    public boolean accepts(BasePsiElement element) {
        // TODO cleanup (nested DATASET structures skip drilling further into the element)
        //DBObjectType virtualObjectType = element.getElementType().getVirtualObjectType();
        //return parentObjectType == null || virtualObjectType == null || !parentObjectType.matches(virtualObjectType);
        return true;
    }

    @Override
    public boolean matches(BasePsiElement basePsiElement) {
        DBObjectType virtualObjectType = basePsiElement.elementType.virtualObjectType;
        return virtualObjectType != null && virtualObjectType.matches(objectType);
    }

/*    private int getLevel(DBObjectType objectType) {
        switch (objectType) {
            case DATASET:
            case CURSOR:
            case TYPE: return 0;
            case COLUMN:
            case TYPE_ATTRIBUTE: return 1;
            default: throw new IllegalArgumentException("Level not defined for object type " + objectType);
        }
    }*/

}
