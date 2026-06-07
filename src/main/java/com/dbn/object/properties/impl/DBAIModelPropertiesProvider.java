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

package com.dbn.object.properties.impl;

import com.dbn.object.DBAIModel;
import com.dbn.object.properties.DBObjectProperty;
import com.dbn.object.properties.SimplePresentableProperty;
import com.dbn.object.type.DBObjectType;

import java.util.List;

import static com.dbn.nls.NlsResources.txt;

public class DBAIModelPropertiesProvider extends DBGenericObjectPropertiesProvider<DBAIModel> {
    public DBAIModelPropertiesProvider() {
        super(DBObjectType.AI_MODEL);
    }

    @Override
    public List<DBObjectProperty> getProperties(DBAIModel model) {
        List<DBObjectProperty> properties = super.getProperties(model);
        properties.add(0, new SimplePresentableProperty(txt("app.objects.property.MiningFunction"), model.getMiningFunction()));
        properties.add(1, new SimplePresentableProperty(txt("app.objects.property.Algorithm"), model.getAlgorithm()));
        properties.add(2, new SimplePresentableProperty(txt("app.objects.property.AlgorithmType"), model.getAlgorithmType()));
        properties.add(3, new SimplePresentableProperty(
                txt("app.objects.property.ModelSize"),
                txt("app.objects.propertyValue.Megabytes", model.getModelSize())));
        properties.add(4, new SimplePresentableProperty(txt("app.objects.property.Partitioned"), model.isPartitioned()));
        properties.add(5, new SimplePresentableProperty(txt("app.objects.property.InMemory"), model.isInmemory()));
        properties.add(5, new SimplePresentableProperty(txt("app.objects.property.ExternalData"), model.isExternalData()));
        return properties;
    }
}
