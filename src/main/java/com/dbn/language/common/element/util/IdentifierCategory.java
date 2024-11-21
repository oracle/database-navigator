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

package com.dbn.language.common.element.util;

import com.dbn.common.util.Enumerations;
import com.dbn.language.common.psi.IdentifierPsiElement;

public enum IdentifierCategory {
    DEFINITION,
    REFERENCE,
    UNKNOWN,
    ALL;

    public boolean matches(IdentifierPsiElement identifierPsiElement) {
        switch (this) {
            case DEFINITION: return identifierPsiElement.isDefinition();
            case REFERENCE: return identifierPsiElement.isReference();
            default: return true;
        }
    }

    public boolean isOneOf(IdentifierCategory... categories) {
        return Enumerations.isOneOf(this, categories);
    }
}
