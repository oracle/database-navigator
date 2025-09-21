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

package com.dbn.assistant.profile;

import com.dbn.common.options.PersistentConfiguration;
import com.dbn.common.ui.Presentable;
import com.dbn.common.util.Cloneable;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import java.util.UUID;

import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Commons.nvl;

@Getter
@Setter
public class AssistantProfile implements PersistentConfiguration, Presentable, Cloneable<AssistantProfile> {
    private String id = UUID.randomUUID().toString();
    private String name;
    private String provider;
    private String credentialId;

    public void readConfiguration(Element element) {
        id = nvl(stringAttribute(element, "id"), id);
        name = stringAttribute(element, "name");
        provider = stringAttribute(element, "provider");
        credentialId = stringAttribute(element, "credential-id");
    }

    @Override
    public void writeConfiguration(Element element) {
        setStringAttribute(element, "id", id);
        setStringAttribute(element, "name", name);
        setStringAttribute(element, "provider", provider);
        setStringAttribute(element, "credential-id", credentialId);
    }

    @Override
    public AssistantProfile clone() {
        AssistantProfile clone = new AssistantProfile();
        clone.id = id;
        clone.name = name;
        clone.provider = provider;
        clone.credentialId = credentialId;
        return clone;
    }
}
