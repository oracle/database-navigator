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

import com.dbn.common.message.TitledMessage;
import com.dbn.common.state.PersistentStateElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.integerAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setIntegerAttribute;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessageNoteSection implements ChatMessageSection, PersistentStateElement {
    private int offset; // offset in the original assistant message

    private TitledMessage message;
    private boolean folded = true;

    public ChatMessageNoteSection(int offset, TitledMessage message) {
        this.offset = offset;
        this.message = message;
    }

    @Override
    public ChatMessageSectionType getType() {
        return ChatMessageSectionType.NOTE;
    }

    @Override
    public void readState(Element element) {
        offset = integerAttribute(element, "offset", offset);
        Element messageElement = element.getChild("message");

        message = new TitledMessage();
        message.readState(messageElement);
    }

    @Override
    public void writeState(Element element) {
        setIntegerAttribute(element, "offset", offset);

        Element messageElement = newElement(element, "message");
        message.writeState(messageElement);
    }
}
