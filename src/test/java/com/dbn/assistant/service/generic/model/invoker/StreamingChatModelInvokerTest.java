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

package com.dbn.assistant.service.generic.model.invoker;

import com.dbn.assistant.adapter.AssistantResponseConsumer;
import org.junit.Assert;
import org.junit.Test;

public class StreamingChatModelInvokerTest {
    @Test
    public void tokenBufferFlushesLongPunctuationRunsAtMaximumLength() {
        StreamingChatModelInvoker.TokenBuffer buffer = new StreamingChatModelInvoker.TokenBuffer();
        RecordingResponseConsumer consumer = new RecordingResponseConsumer();
        int tokenLength = StreamingChatModelInvoker.MAX_TOKEN_BUFFER_LENGTH * 3;

        for (int i = 0; i < tokenLength; i++) {
            buffer.append("*");
            buffer.consume(consumer, false);

            Assert.assertTrue(buffer.length() < StreamingChatModelInvoker.MAX_TOKEN_BUFFER_LENGTH);
        }

        buffer.consume(consumer, true);

        Assert.assertTrue(consumer.tokenCount >= 3);
        Assert.assertEquals(tokenLength, consumer.totalTokenLength);
        Assert.assertEquals(0, buffer.length());
    }

    @Test
    public void tokenBufferFlushesImmediatelyWhenForced() {
        StreamingChatModelInvoker.TokenBuffer buffer = new StreamingChatModelInvoker.TokenBuffer();
        RecordingResponseConsumer consumer = new RecordingResponseConsumer();

        buffer.append("hello");
        buffer.consume(consumer, true);

        Assert.assertEquals(1, consumer.tokenCount);
        Assert.assertEquals("hello", consumer.lastToken);
        Assert.assertEquals(0, buffer.length());
    }

    private static class RecordingResponseConsumer implements AssistantResponseConsumer {
        private int tokenCount;
        private int totalTokenLength;
        private String lastToken;

        @Override
        public void acceptToken(String token) {
            tokenCount++;
            totalTokenLength += token.length();
            lastToken = token;
        }

        @Override
        public void acceptMessage(String message) {
        }

        @Override
        public void acceptError(String message, Throwable exception) {
        }

        @Override
        public void acceptToolError(String message, Throwable exception) {
        }

        @Override
        public void acceptToolRequest(String requestId, String toolName, String toolArguments) {
        }

        @Override
        public void acceptToolResponse(String requestId, String toolName, String toolResponse) {
        }

        @Override
        public void acceptCompletion() {
        }
    }
}
