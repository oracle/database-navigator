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

package com.dbn.assistant.mcp;

import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.state.AssistantStateExtension;
import com.dbn.common.state.PersistentStateElement;
import lombok.Getter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.action.UserDataKeys.ASSISTANT_MCP_SERVER_OPTIONS;
import static com.dbn.common.action.UserDataKeys.getUserDataSync;
import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setBooleanAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;


@Getter
public class AssistantMcpServerOptions extends AssistantStateExtension implements PersistentStateElement {
    private final Map<String, Boolean> selections = new HashMap<>();

    protected AssistantMcpServerOptions(@NotNull AssistantState assistantState) {
        super(assistantState);
    }

    public static AssistantMcpServerOptions get(AssistantState assistantState) {
        return getUserDataSync(assistantState, ASSISTANT_MCP_SERVER_OPTIONS,
                () -> new AssistantMcpServerOptions(assistantState));
    }

    public boolean isSelected(String id) {
        Boolean selected = selections.get(id);
        return selected != null && selected;
    }

    public void setSelected(String id, boolean selected) {
        selections.put(id, selected);
    }

    public int countSelected() {
        return (int) selections.values().stream().filter(b -> b).count();
    }

    @Override
    public void readState(Element element) {
        if (element == null) return;

        Element mcpServersElement = element.getChild("selections");
        List<Element> mcpServerElements = childrenOf(mcpServersElement, "mcp-server");
        for (Element mcpServerElement : mcpServerElements) {
            String serverId = stringAttribute(mcpServerElement, "id");
            boolean selected = booleanAttribute(mcpServerElement, "selected", false);
            selections.put(serverId, selected);
        }
    }

    @Override
    public void writeState(Element element) {
        if (element == null) return;

        if (!selections.isEmpty()) {
            Element approvalsElement = newElement(element, "selections");
            for (String serverId : selections.keySet()) {
                boolean selected = selections.get(serverId);
                Element serverElement = newElement(approvalsElement, "mcp-server");
                setStringAttribute(serverElement, "id", serverId);
                setBooleanAttribute(serverElement, "selected", selected);
            }
        }

    }
}
