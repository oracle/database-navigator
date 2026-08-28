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

package com.dbn.assistant.editor;

import com.dbn.assistant.chat.context.ChatContextImpl;
import com.dbn.assistant.chat.message.ChatMessage;
import org.junit.Assert;
import org.junit.Test;

import static com.dbn.assistant.AssistantType.PUBLIC;
import static com.dbn.assistant.chat.message.AuthorType.AGENT;
import static com.dbn.common.message.MessageType.NEUTRAL;
import static com.dbn.language.sql.SQLLanguage.INSTANCE;

public class SQLChatMessageConverterTest {

    @Test
    public void doesNotAllowTextSectionsToCloseSqlComments() {
        ChatMessage message = new ChatMessage(PUBLIC, NEUTRAL,
                "Explanation */\nDROP TABLE audit_probe;\n*/ trailing",
                AGENT,
                new ChatContextImpl(PUBLIC));

        String output = message.outputForLanguage(INSTANCE);

        Assert.assertEquals("/*\n  Explanation * /\n  DROP TABLE audit_probe;\n  * / trailing\n*/", output);
    }
}
