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

package com.dbn.assistant.chat.message;

import com.dbn.assistant.tool.execution.AssistantToolInvocation;
import com.dbn.common.state.PersistentStateElement;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.integerAttribute;
import static com.dbn.common.options.setting.Settings.setIntegerAttribute;

@Getter
@Setter
public class ChatMessageToolSection implements PersistentStateElement {
    private int offset; // offset in the original assistant message

    @Delegate
    private AssistantToolInvocation invocation;

    public ChatMessageToolSection() {}

    public ChatMessageToolSection(int offset, AssistantToolInvocation invocation) {
        this.offset = offset;
        this.invocation = invocation;
    }

    @Override
    public void readState(Element element) {
        offset = integerAttribute(element, "offset", offset);
        invocation = new AssistantToolInvocation();
        invocation.readState(element);
    }

    @Override
    public void writeState(Element element) {
        setIntegerAttribute(element, "offset", offset);
        invocation.writeState(element);
    }

    public String getToolName() {
        return invocation.getRequest().getUtility();
    }

    public String getToolRequestId() {
        return invocation.getRequest().getRequestId();
    }
}
