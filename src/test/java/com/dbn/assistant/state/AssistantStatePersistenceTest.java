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

package com.dbn.assistant.state;

import com.dbn.connection.ConnectionId;
import org.jdom.Element;
import org.junit.Assert;
import org.junit.Test;

import static com.dbn.assistant.AssistantType.PUBLIC;

public class AssistantStatePersistenceTest {
    @Test
    public void ignoresExcessiveChatsOnStateLoad() {
        Element assistantStateElement = assistantStateElement();
        Element chatsElement = new Element("chats");
        for (int i = 0; i <= AssistantState.MAX_CHAT_COUNT; i++) {
            chatsElement.addContent(new Element("chat"));
        }
        assistantStateElement.addContent(chatsElement);

        AssistantState state = readState(assistantStateElement);

        Assert.assertTrue(state.getChats().isEmpty());
    }

    @Test
    public void loadsChatsWithinLimit() {
        Element assistantStateElement = assistantStateElement();
        Element chatsElement = new Element("chats");
        Element chatElement = new Element("chat");
        chatElement.setAttribute("id", "chat-1");
        chatElement.addContent(new Element("messages"));
        chatElement.addContent(new Element("context"));
        chatsElement.addContent(chatElement);
        assistantStateElement.addContent(chatsElement);

        AssistantState state = readState(assistantStateElement);

        Assert.assertEquals(1, state.getChats().size());
    }

    private static AssistantState readState(Element element) {
        AssistantState state = new AssistantState();
        state.readState(element);
        return state;
    }

    private static Element assistantStateElement() {
        Element element = new Element("assistant-state");
        element.setAttribute("connection-id", ConnectionId.get("test-connection").id());
        element.setAttribute("assistant-type", PUBLIC.name());
        element.addContent(new Element("tools"));
        element.addContent(new Element("mcp-servers"));
        return element;
    }
}
