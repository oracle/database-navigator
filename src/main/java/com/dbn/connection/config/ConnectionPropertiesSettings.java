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

package com.dbn.connection.config;

import com.dbn.common.options.BasicProjectConfiguration;
import com.dbn.common.state.ProtectedContent;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.ui.ConnectionPropertiesSettingsForm;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.getBoolean;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setBoolean;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.state.StateEncryptionScopes.CONNECTION_GENERIC_PROPERTY;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class ConnectionPropertiesSettings extends BasicProjectConfiguration<ConnectionSettings, ConnectionPropertiesSettingsForm> {
    private static final int MAX_PROPERTY_COUNT = 512;
    private Map<String, String> properties = new HashMap<>();
    private boolean enableAutoCommit = false;

    ConnectionPropertiesSettings(ConnectionSettings parent) {
        super(parent);
    }

    @NotNull
    @Override
    public ConnectionPropertiesSettingsForm createConfigurationEditor() {
        return new ConnectionPropertiesSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "properties";
    }

    @NotNull
    public ConnectionId getConnectionId() {
        return getParent().getConnectionId();
    }

    /*********************************************************
    *                 PersistentConfiguration               *
    *********************************************************/
    @Override
    public void readConfiguration(Element element) {
        Map<String, String> properties = new HashMap<>();
        Element propertiesElement = element.getChild("properties");
        for (Element propertyElement : childrenOf(propertiesElement, "property", MAX_PROPERTY_COUNT)) {
            String key = stringAttribute(propertyElement, "key");
            if (key == null) continue;

            ProtectedContent value = new ProtectedContent(CONNECTION_GENERIC_PROPERTY);
            Element valueElement = propertyElement.getChild("value");
            if (valueElement == null) {
                // Legacy settings stored property values as plaintext attributes.
                value.set(stringAttribute(propertyElement, "value"));
            } else {
                value.readState(valueElement);
            }
            properties.put(key, value.get());
        }

        this.properties = properties;
        this.enableAutoCommit = getBoolean(element, "auto-commit", enableAutoCommit);

        getParent().getDatabaseSettings().updateSignature();
    }

    @Override
    public void writeConfiguration(Element element) {
        setBoolean(element, "auto-commit", enableAutoCommit);
        if (properties.isEmpty()) return;

        Element propertiesElement = newElement(element, "properties");
        for (var entry : properties.entrySet()) {
            String key = entry.getKey();
            if (key == null) continue;

            Element propertyElement = newElement(propertiesElement, "property");
            propertyElement.setAttribute("key", key);

            Element valueElement = newElement(propertyElement, "value");
            ProtectedContent value = new ProtectedContent(CONNECTION_GENERIC_PROPERTY, entry.getValue());
            value.writeState(valueElement);
        }
    }
}
