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

package com.dbn.assistant.tool.execution;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.state.ProtectedContent;
import lombok.NoArgsConstructor;
import org.jdom.Element;

import static com.dbn.assistant.tool.AssistantToolContents.prepareToolResponseContent;
import static com.dbn.common.state.StateEncryptionScopes.ASSISTANT_TOOL_RESPONSE;

@NoArgsConstructor
public class AssistantToolResponse implements PersistentStateElement {
    private final ProtectedContent content = new ProtectedContent(ASSISTANT_TOOL_RESPONSE);

    public AssistantToolResponse(String content) {
        setContent(content);
    }

    public String getContent() {
        return content.get();
    }

    public void setContent(String content) {
        this.content.set(prepareToolResponseContent(content));
    }

    @Override
    public void readState(Element element) {
        content.readState(element);
    }

    @Override
    public void writeState(Element element) {
        content.writeState(element);
    }

}
