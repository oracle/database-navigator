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

package com.dbn.assistant.mcp.model;

import com.dbn.common.EntityId;
import com.dbn.common.options.PersistentConfiguration;
import com.dbn.common.ui.Presentable;
import com.dbn.common.util.Cloneable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.constantAttribute;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.setConstantAttribute;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Unsafe.cast;

@Getter
@Setter
@NoArgsConstructor
public class AssistantMcpServer implements PersistentConfiguration, Presentable, Cloneable<AssistantMcpServer> {
    private AssistantMcpServerType type = AssistantMcpServerType.HTTP;
    private EntityId id;
    private String name;
    private String key;
    private String url;
    private String command;

    public AssistantMcpServer(EntityId id) {
        this.id = id;
    }

    public String getEndpoint() {
        return switch (type) {
            case HTTP -> url;
            case STDIO -> command;
        };
    }

    public void setName(String name) {
        this.name = name;
        this.key = name.trim().toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_");
    }

    public boolean matchesUtilityName(String utilityName) {
        return utilityName.startsWith(key + "_");
    }

    public static String qualifiedUtilityName(String serverKey, String utilityName) {
        return serverKey + "_" + utilityName;
    }

    public String unqualifiedUtilityName(String utilityName) {
        if (matchesUtilityName(utilityName)) {
            return utilityName.substring(key.length() + 1);
        }
        return utilityName;
    }

    @Override
    public void readConfiguration(Element element) {
        id = constantAttribute(element, "id", EntityId.class);

        type = enumAttribute(element, "type", AssistantMcpServerType.class);
        name = stringAttribute(element, "name");
        key = stringAttribute(element, "key");
        url = stringAttribute(element, "url");
        command = stringAttribute(element, "command");

    }

    @Override
    public void writeConfiguration(Element element) {
        setConstantAttribute(element, "id", id);

        setEnumAttribute(element, "type", type);
        setStringAttribute(element, "name", name);
        setStringAttribute(element, "key", key);
        setStringAttribute(element, "url", url);
        setStringAttribute(element, "command", command);
    }

    @Override
    @SneakyThrows
    public AssistantMcpServer clone() {
        return cast(super.clone());
    }
}
