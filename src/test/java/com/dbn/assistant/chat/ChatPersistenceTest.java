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

package com.dbn.assistant.chat;

import com.dbn.assistant.chat.context.ChatContextImpl;
import org.jdom.Element;
import org.junit.Assert;
import org.junit.Test;

import static com.dbn.assistant.AssistantType.PUBLIC;

public class ChatPersistenceTest {
    @Test
    public void ignoresExcessiveMessagesOnStateLoad() {
        Chat chat = new Chat(new ChatContextImpl(PUBLIC));
        Element chatElement = new Element("chat");
        Element messagesElement = new Element("messages");
        for (int i = 0; i <= Chat.MAX_MESSAGE_COUNT; i++) {
            messagesElement.addContent(new Element("message"));
        }
        chatElement.addContent(messagesElement);
        chatElement.addContent(new Element("context"));

        chat.readState(chatElement);

        Assert.assertTrue(chat.getMessages().isEmpty());
    }

    @Test
    public void loadsMessagesWithinLimit() {
        Chat chat = new Chat(new ChatContextImpl(PUBLIC));
        Element chatElement = new Element("chat");
        Element messagesElement = new Element("messages");
        messagesElement.addContent(messageElement());
        chatElement.addContent(messagesElement);
        chatElement.addContent(new Element("context"));

        chat.readState(chatElement);

        Assert.assertEquals(1, chat.getMessages().size());
    }

    private static Element messageElement() {
        Element messageElement = new Element("message");
        messageElement.addContent(new Element("content"));
        messageElement.addContent(new Element("context"));
        return messageElement;
    }
}
