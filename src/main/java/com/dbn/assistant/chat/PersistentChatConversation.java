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

package com.dbn.assistant.chat;

import com.dbn.assistant.chat.message.PersistentChatMessage;
import com.dbn.common.state.PersistentStateElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jdom.Element;

import java.util.List;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setBooleanAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;


/**
 * This class is for conversation elements that will be in the chat
 */
@Getter
@Setter
@NoArgsConstructor
public class PersistentChatConversation extends ChatConversation implements PersistentStateElement {

    /**
     * Creates a new ChatConversation
     *
     * @param context the context in which the chat conversation was produced
     */
    public PersistentChatConversation(List<PersistentChatMessage> chatMessageList, ChatConversationContext context) {
        super(chatMessageList, context);
    }

    @Override
    public void readState(Element element) {
        chatTitle = stringAttribute(element, "title");
        active = booleanAttribute(element, "active", false);
        List<Element> messagesElements = element.getChild("messages").getChildren();
        for(Element msgElement : messagesElements){
            PersistentChatMessage chatMessage = new PersistentChatMessage();
            chatMessage.readState(msgElement);
            messages.add(chatMessage);
        }
        Element contextElement = element.getChild("context");
        context = new ChatConversationContext();
        context.readState(contextElement);
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "title", chatTitle);
        setBooleanAttribute(element, "active", active);
        Element messagesElement = newElement("messages");
        for(PersistentChatMessage msg : messages){
            Element msgElement = newElement("message");
            messagesElement.addContent(msgElement);
            msg.writeState(msgElement);
        }

        Element contextElement = newElement(element,"context");
        context.writeState(contextElement);
    }

}


