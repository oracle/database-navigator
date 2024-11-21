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

import com.dbn.common.property.PropertyHolderBase;

import java.util.Objects;
import java.util.StringTokenizer;

public class ElementTypeAttributeHolder extends PropertyHolderBase.LongStore<ElementTypeAttribute> {

    @Override
    protected ElementTypeAttribute[] properties() {
        return ElementTypeAttribute.VALUES;
    }

    public ElementTypeAttributeHolder(String definition) throws ElementTypeDefinitionException {
        StringTokenizer tokenizer = new StringTokenizer(definition, ",");
        while (tokenizer.hasMoreTokens()) {
            String attributeName = tokenizer.nextToken().trim();
            boolean found = false;
            for (ElementTypeAttribute attribute : ElementTypeAttribute.values()) {
                if (Objects.equals(attribute.getName(), attributeName)) {
                    set(attribute, true);
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new ElementTypeDefinitionException("Invalid element type attribute '" + attributeName + "'");
            }
        }
    }
}
