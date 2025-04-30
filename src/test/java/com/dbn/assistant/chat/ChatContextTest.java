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

import com.dbn.assistant.chat.window.PromptAction;
import com.dbn.assistant.provider.AIModel;
import org.junit.Assert;
import org.junit.Test;

public class ChatContextTest {

//    @Test
//    public void isConversationInterruption_ProfileSwitch() {
//        ChatContext oldContext = new ChatContext("profile1", AIModel.forId("GPT_4_O"), PromptAction.CHAT, true);
//        ChatContext newContext = new ChatContext("profile2", AIModel.forId("GPT_4_O"), PromptAction.CHAT, true);
//
//        Assert.assertEquals("profile", oldContext.isConversationInterruption(newContext, null, false));
//
//    }
//
//    @Test
//    public void isConversationInterruption_ModelSwitch() {
//        ChatContext oldContext = new ChatContext("profile1", AIModel.forId("GPT_4_O"), PromptAction.CHAT, true);
//        ChatContext newContext = new ChatContext("profile1", AIModel.forId("GPT_4_O_MINI"), PromptAction.CHAT, true);
//
//        Assert.assertEquals("model", oldContext.isConversationInterruption(newContext, null, false));
//    }
//    @Test
//    public void isConversationInterruption_ActionSwitch1() {
//        ChatContext oldContext = new ChatContext("profile1", AIModel.forId("GPT_4_O"), PromptAction.CHAT, true);
//        ChatContext newContext = new ChatContext("profile1", AIModel.forId("GPT_4_O"), PromptAction.SHOW_SQL, true);
//
//        Assert.assertEquals("conversation type", oldContext.isConversationInterruption(newContext, null, false));
//    }
//    @Test
//    public void isConversationInterruption_ActionSwitch2() {
//        ChatContext oldContext = new ChatContext("profile1", AIModel.forId("GPT_4_O"), PromptAction.SHOW_SQL, true);
//        ChatContext newContext = new ChatContext("profile1", AIModel.forId("GPT_4_O"), PromptAction.CHAT, true);
//
//        Assert.assertEquals("conversation type", oldContext.isConversationInterruption(newContext, null, false));
//    }
//    @Test
//    public void isConversationInterruption_ActionSwitch3() {
//        ChatContext oldContext = new ChatContext("profile1", AIModel.forId("GPT_4_O"), PromptAction.SHOW_SQL, true);
//        ChatContext newContext = new ChatContext("profile1", AIModel.forId("GPT_4_O"), PromptAction.EXPLAIN_SQL, true);
//
//        Assert.assertEquals("", oldContext.isConversationInterruption(newContext, null, false));
//    }
}