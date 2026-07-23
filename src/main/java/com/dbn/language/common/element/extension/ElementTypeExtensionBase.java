/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.language.common.element.extension;

import com.dbn.common.util.Lists;
import com.dbn.common.util.Strings;
import com.dbn.language.common.element.impl.ElementTypeBase;
import org.jdom.Element;

import java.util.List;

import static com.dbn.common.options.setting.Settings.integerAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

public abstract class ElementTypeExtensionBase<E extends ElementTypeBase> {
    public final E elementType;
    public final String id;
    public final int depth;

    protected ElementTypeExtensionBase(E elementType, Element definition) {
        this.elementType = elementType;
        this.id = stringAttribute(definition, "id");
        this.depth = integerAttribute(definition, "depth", 0);
    }

    protected static List<String> csvAttribute(Element element, String name) {
        String value = stringAttribute(element, name);
        if (Strings.isEmpty(value)) return List.of();

        return Lists.fromCsv(value);
    }
}
