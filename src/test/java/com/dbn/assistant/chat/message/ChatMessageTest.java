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

import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class ChatMessageTest {

    @Test
    public void testConcurrency() throws Exception{
        String content = readResource("/assistantChatMessages/md-sample-1.md");
        // TODO
        System.out.println(content);
    }

    private String readResource(String name) throws IOException {
        URL resource = getClass().getResource(name);
        return Files.readString(Path.of(resource.getPath()));
    }
}