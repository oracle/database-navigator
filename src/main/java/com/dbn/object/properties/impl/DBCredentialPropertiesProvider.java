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

import com.dbn.object.DBCredential;
import com.dbn.object.properties.DBObjectProperty;
import com.dbn.object.properties.SimplePresentableProperty;
import com.dbn.object.type.DBObjectType;

import java.util.List;

public class DBCredentialPropertiesProvider extends DBGenericObjectPropertiesProvider<DBCredential> {
    public DBCredentialPropertiesProvider() {
        super(DBObjectType.CREDENTIAL);
    }

    @Override
    public List<DBObjectProperty> getProperties(DBCredential credential) {
        List<DBObjectProperty> properties = super.getProperties(credential);
        properties.add(0, new SimplePresentableProperty("Credential type", credential.getType()));
        properties.add(1, new SimplePresentableProperty("User name", credential.getUserName()));
        properties.add(2, new SimplePresentableProperty("Enabled", credential.isEnabled()));
        return properties;
    }
}
