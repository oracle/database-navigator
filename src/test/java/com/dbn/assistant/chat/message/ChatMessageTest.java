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

package com.dbn.assistant.chat.message;

import com.dbn.common.message.MessageType;
import org.junit.Test;
import org.locationtech.jts.util.Assert;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ChatMessageTest {

    @Test
    public void testSample1() throws Exception{
        List<ChatMessageSection> sections = readMessageSections("/assistantChatMessages/md-sample-01.md");
        Assert.equals(1, sections.size());
    }

    @Test
    public void testSample2() throws Exception{
        List<ChatMessageSection> sections = readMessageSections("/assistantChatMessages/md-sample-02.md");
        Assert.equals(1, sections.size());
    }

    private static List<ChatMessageSection> readMessageSections(String resource) throws IOException {
        String content = readResource(resource);
        ChatMessage chatMessage = new ChatMessage(MessageType.NEUTRAL, content, AuthorType.AGENT, new ChatMessageContext());
        return chatMessage.getSections();
    }

    private static String readResource(String name) throws IOException {
        URL resource = ChatMessageTest.class.getResource(name);
        return Files.readString(Path.of(resource.getPath()));
    }
}