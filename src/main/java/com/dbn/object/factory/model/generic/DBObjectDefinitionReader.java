/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.object.factory.model.generic;

import com.dbn.common.data.Data;
import com.dbn.object.type.DBObjectType;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.jdom.Element;

import java.util.List;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@UtilityClass
public class DBObjectDefinitionReader {

    @SneakyThrows
    public static DBObjectDefinition read(Element element) {
        DBObjectDefinition definition = readDefinition(element);

        readAttributes(element, definition);
        radChildren(element, definition);

        return definition;
    }

    private static DBObjectDefinition readDefinition(Element element) {
        DBObjectType objectType = enumAttribute(element, "type", DBObjectType.class);
        String objectName = stringAttribute(element, "name");
        boolean readonly = booleanAttribute(element, "readonly", false);

        DBObjectDefinition definition = new DBObjectDefinition(objectType);
        definition.setObjectName(objectName);
        definition.setReadonly(readonly);
        return definition;
    }

    private static void radChildren(Element element, DBObjectDefinition definition) {
        List<Element> childElements = childrenOf(element.getChild("children"));
        for (Element childElement : childElements) {
            DBObjectDefinition childDefinition = readDefinition(childElement);
            definition.addChild(childDefinition);
        }
    }

    private static void readAttributes(Element element, DBObjectDefinition definition) {
        List<Element> attributeElements = childrenOf(element.getChild("attributes"));
        for (Element attributeElement : attributeElements) {
            readAttribute(attributeElement, definition);
        }
    }

    private static void readAttribute(Element element, DBObjectDefinition definition) {
        String name = stringAttribute(element, "name");
        String stringValue = stringAttribute(element, "value");
        DBObjectAttribute<Object> attribute = DBObjectAttribute.get(name);
        Class<Object> type = attribute.getType();

        Object value = Data.asType(stringValue, type);
        definition.addAttribute(attribute, value);
    }
}
