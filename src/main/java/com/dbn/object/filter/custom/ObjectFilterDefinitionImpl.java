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

package com.dbn.object.filter.custom;

import com.dbn.object.common.DBObject;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.dbn.common.util.Lists.convert;

@Getter
final class ObjectFilterDefinitionImpl<T extends DBObject> implements ObjectFilterDefinition<T> {

    private final DBObjectType objectType;
    private final String sampleExpression;
    private final List<ObjectFilterAttribute> attributes = new ArrayList<>();
    private final Map<String, Function<T, Object>> valueProviders = new LinkedHashMap<>();

    public ObjectFilterDefinitionImpl(DBObjectType objectType, String sampleExpression) {
        this.objectType = objectType;
        this.sampleExpression = sampleExpression;
    }

    @Override
    public List<String> getAttributeNames() {
        return convert(attributes, a -> a.getName());
    }

    @Override
    public Object getAttributeValue(T source, String attribute) {
        var valueProvider = valueProviders.get(attribute);

        return valueProvider == null ? null : valueProvider.apply(source);
    }

    public ObjectFilterDefinitionImpl<T> withAttribute(Class type, String name, String description, Function<T, Object> valueProvider) {
        ObjectFilterAttribute attribute = new ObjectFilterAttribute(type, name, description);
        attributes.add(attribute);
        valueProviders.put(name, valueProvider);
        return this;
    }
}
