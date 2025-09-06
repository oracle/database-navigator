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

import com.dbn.common.state.PersistentStateElement;
import lombok.Data;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.integerAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.readCdata;
import static com.dbn.common.options.setting.Settings.setIntegerAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.options.setting.Settings.writeCdata;

@Data
public class ChatMessageToolSection implements PersistentStateElement {
    private int offset; // offset in the original assistant message
    private String requestId;
    private String toolName;
    private String toolArguments;
    private String toolResponse;

    public ChatMessageToolSection() {}

    public ChatMessageToolSection(int offset, String requestId, String toolName, String toolArguments) {
        this.offset = offset;
        this.requestId = requestId;
        this.toolName = toolName;
        this.toolArguments = toolArguments;
    }

    @Override
    public void readState(Element element) {
        offset = integerAttribute(element, "offset", offset);
        requestId = stringAttribute(element, "request-id");
        toolName = stringAttribute(element, "tool-name");

        Element contentElement = element.getChild("tool-arguments");
        toolArguments = readCdata(contentElement);

        Element responseElement = element.getChild("tool-response");
        toolResponse = readCdata(responseElement);
    }

    @Override
    public void writeState(Element element) {
        setIntegerAttribute(element, "offset", offset);
        setStringAttribute(element, "request-id", requestId);
        setStringAttribute(element, "tool-name", toolName);

        Element contentElement = newElement(element,"tool-arguments");
        writeCdata(contentElement, toolArguments);

        Element resposeElement = newElement(element,"tool-response");
        writeCdata(resposeElement, toolResponse);
    }
}
