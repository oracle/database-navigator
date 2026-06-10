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

package com.dbn.assistant;

import org.junit.Assert;
import org.junit.Test;

public class AssistantErrorMessagesTest {
    @Test
    public void sanitizeUrlsForDisplayRemovesQueryStringsAndFragments() {
        String message = "Request failed: https://assistant.example.com/mcp?token=secret#private-fragment";

        String sanitized = AssistantErrorMessages.sanitizeUrls(message);

        Assert.assertEquals("Request failed: https://assistant.example.com", sanitized);
        Assert.assertFalse(sanitized.contains("mcp"));
        Assert.assertFalse(sanitized.contains("token=secret"));
        Assert.assertFalse(sanitized.contains("private-fragment"));
    }

    @Test
    public void sanitizeUrlsForDisplayRemovesEmbeddedCredentials() {
        String message = "Endpoint https://tenant:password@internal.example.com/route failed";

        String sanitized = AssistantErrorMessages.sanitizeUrls(message);

        Assert.assertEquals("Endpoint https://internal.example.com failed", sanitized);
        Assert.assertFalse(sanitized.contains("tenant:password"));
        Assert.assertFalse(sanitized.contains("route"));
    }

    @Test
    public void sanitizeUrlsForDisplayRemovesSignedUrls() {
        String message = "Download failed for https://storage.example.com/blob?X-Amz-Credential=tenant&X-Amz-Signature=secret";

        String sanitized = AssistantErrorMessages.sanitizeUrls(message);

        Assert.assertEquals("Download failed for https://storage.example.com", sanitized);
        Assert.assertFalse(sanitized.contains("blob"));
        Assert.assertFalse(sanitized.contains("X-Amz-Credential"));
        Assert.assertFalse(sanitized.contains("X-Amz-Signature"));
    }

    @Test
    public void sanitizeUrlsForDisplayPreservesPorts() {
        String message = "Endpoint http://user:password@localhost:8080/tenant/path?token=secret failed";

        String sanitized = AssistantErrorMessages.sanitizeUrls(message);

        Assert.assertEquals("Endpoint http://localhost:8080 failed", sanitized);
        Assert.assertFalse(sanitized.contains("user:password"));
        Assert.assertFalse(sanitized.contains("tenant"));
        Assert.assertFalse(sanitized.contains("token=secret"));
    }

    @Test
    public void sanitizeUrlsForDisplayPreservesProviderApiPaths() {
        String message = "Request failed: https://user:secret@api.openai.com/v1/chat/completions?api-key=secret#fragment";

        String sanitized = AssistantErrorMessages.sanitizeUrls(message);

        Assert.assertEquals("Request failed: https://api.openai.com/v1/chat/completions", sanitized);
        Assert.assertFalse(sanitized.contains("user:secret"));
        Assert.assertFalse(sanitized.contains("api-key=secret"));
        Assert.assertFalse(sanitized.contains("fragment"));
    }

    @Test
    public void sanitizeUrlsForDisplayStripsUnknownApiPaths() {
        String message = "Request failed: https://assistant.internal.example.com/v1/chat/completions?token=secret";

        String sanitized = AssistantErrorMessages.sanitizeUrls(message);

        Assert.assertEquals("Request failed: https://assistant.internal.example.com", sanitized);
        Assert.assertFalse(sanitized.contains("v1"));
        Assert.assertFalse(sanitized.contains("token=secret"));
    }

    @Test
    public void sanitizeUrlsForDisplayHandlesNullMessages() {
        Assert.assertNull(AssistantErrorMessages.sanitizeUrls(null));
    }
}
