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

package com.dbn.assistant.tool.execution;

import org.junit.Assert;
import org.junit.Test;

import static com.dbn.assistant.tool.AssistantToolContents.getMaxToolResponseLength;
import static com.dbn.assistant.tool.AssistantToolContents.isToolResponseContentOversized;

public class AssistantToolResponseTest {
    private static final String TOOL_RESPONSE_TRUNCATED_SUFFIX = "\n[Tool response truncated]";

    @Test
    public void truncatesOversizedContent() {
        AssistantToolResponse response = new AssistantToolResponse("x".repeat(getMaxToolResponseLength() + 1));

        String content = response.getContent();
        Assert.assertEquals(getMaxToolResponseLength(), content.length());
        Assert.assertTrue(content.endsWith(TOOL_RESPONSE_TRUNCATED_SUFFIX));
    }

    @Test
    public void identifiesOversizedContent() {
        Assert.assertFalse(isToolResponseContentOversized("x".repeat(getMaxToolResponseLength())));
        Assert.assertTrue(isToolResponseContentOversized("x".repeat(getMaxToolResponseLength() + 1)));
    }
}
