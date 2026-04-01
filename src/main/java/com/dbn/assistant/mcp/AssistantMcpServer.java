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

package com.dbn.assistant.mcp;

import com.dbn.common.options.PersistentConfiguration;
import com.dbn.common.ui.Presentable;
import com.dbn.common.util.Cloneable;
import com.dbn.common.util.UUIDs;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Unsafe.cast;

@Getter
@Setter
public class AssistantMcpServer implements PersistentConfiguration, Presentable, Cloneable<AssistantMcpServer> {
    private String id = UUIDs.regular();
    private AssistantMcpServerType type = AssistantMcpServerType.HTTP;
    private String name;
    private String url;
    private String command;


    @Override
    public void readConfiguration(Element element) {
        id = stringAttribute(element, "id", id);

        type = enumAttribute(element, "type", AssistantMcpServerType.class);
        name = stringAttribute(element, "name");
        url = stringAttribute(element, "url");
        command = stringAttribute(element, "command");

    }

    @Override
    public void writeConfiguration(Element element) {
        setStringAttribute(element, "id", id);

        setEnumAttribute(element, "type", type);
        setStringAttribute(element, "name", name);
        setStringAttribute(element, "url", url);
        setStringAttribute(element, "command", command);
    }

    @Override
    @SneakyThrows
    public AssistantMcpServer clone() {
        return cast(super.clone());
    }
}
