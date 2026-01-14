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

package com.dbn.object.factory.model;

import com.dbn.common.data.Data;
import com.dbn.common.util.XmlContents;
import com.dbn.object.type.DBObjectType;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@UtilityClass
public class DBObjectSpecReader {

    @SneakyThrows
    public static DBObjectSpec read(Class resourceClass, @NonNls String resourceName) {
        Element element = XmlContents.fileToElement(resourceClass, resourceName);
        return DBObjectSpecReader.read(element);
    }

    public static DBObjectSpec read(Element element) {
        return readDefinition(element);
    }

    private static DBObjectSpec readDefinition(Element element) {
        boolean readonly = booleanAttribute(element, "readonly", false);

        DBObjectSpec definition = new DBObjectSpec();
        definition.setReadonly(readonly);

        readAttributes(element, definition);
        radChildren(element, definition);
        return definition;
    }

    private static void radChildren(Element element, DBObjectSpec definition) {
        Element childrenElement = element.getChild("children");
        boolean readonly = booleanAttribute(childrenElement, "readonly", false);

        Set<DBObjectType> objectTypes = new HashSet<>();
        List<Element> childElements = childrenOf(childrenElement);
        for (Element childElement : childElements) {
            DBObjectSpec childDefinition = readDefinition(childElement);
            definition.addChild(childDefinition);

            DBObjectType objectType = childDefinition.getObjectType();
            objectTypes.add(objectType);
        }

        // TODO child groups in xml definitions (allow individual "readonly" setting)
        objectTypes.forEach(ot -> definition.setChildrenReadonly(ot, readonly));
    }

    private static void readAttributes(Element element, DBObjectSpec definition) {
        List<Element> attributeElements = childrenOf(element, "attribute");
        for (Element attributeElement : attributeElements) {
            readAttribute(attributeElement, definition);
        }
    }

    private static void readAttribute(Element element, DBObjectSpec definition) {
        var attributeName = stringAttribute(element, "name");
        var attributeValue = stringAttribute(element, "value");
        var attributeType = DBObjectAttributeType.get(attributeName);
        var attributeClass = attributeType.getType();

        Object value = Data.asType(attributeValue, attributeClass);
        var attribute = definition.setAttributeValue(attributeType, value);

        boolean readonly = booleanAttribute(element, "readonly", false);
        attribute.setReadonly(readonly);
    }
}
