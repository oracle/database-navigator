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

package com.dbn.assistant.mcp;

import com.dbn.assistant.mcp.model.AssistantMcpServer;
import com.dbn.assistant.mcp.model.AssistantMcpServerType;
import com.dbn.common.EntityId;
import org.junit.Assert;
import org.junit.Test;

public class AssistantMcpServerVerifierTest {

    @Test
    public void verifyAcceptsRemoteHttpsUrl() {
        AssistantMcpServerVerifier.verify(mcpServer("https://8.8.8.8/mcp"));
    }

    @Test
    public void verifyAcceptsLoopbackHttpUrl() {
        AssistantMcpServerVerifier.verify(mcpServer("http://127.0.0.1:3001/mcp"));
        AssistantMcpServerVerifier.verify(mcpServer("http://[::1]:3001/mcp"));
    }

    @Test
    public void verifyAcceptsPrivateHttpUrl() {
        AssistantMcpServerVerifier.verify(mcpServer("http://10.0.0.1/mcp"));
        AssistantMcpServerVerifier.verify(mcpServer("http://172.16.0.1/mcp"));
        AssistantMcpServerVerifier.verify(mcpServer("http://192.168.0.1/mcp"));
    }

    @Test
    public void verifyAcceptsLinkLocalHttpUrl() {
        AssistantMcpServerVerifier.verify(mcpServer("http://169.254.169.254/mcp"));
    }

    @Test
    public void verifyAcceptsUniqueLocalIpv6HttpUrl() {
        AssistantMcpServerVerifier.verify(mcpServer("http://[fd00::1]/mcp"));
    }

    @Test
    public void verifyRejectsRemoteIpv6HttpUrl() {
        IllegalArgumentException exception = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> AssistantMcpServerVerifier.verify(mcpServer("http://[2001:4860:4860::8888]/mcp")));

        Assert.assertTrue(exception.getMessage().contains("HTTPS"));
    }

    @Test
    public void verifyRejectsRemoteHttpUrl() {
        IllegalArgumentException exception = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> AssistantMcpServerVerifier.verify(mcpServer("http://8.8.8.8/mcp")));

        Assert.assertTrue(exception.getMessage().contains("HTTPS"));
    }

    @Test
    public void verifyRejectsUnsupportedScheme() {
        IllegalArgumentException exception = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> AssistantMcpServerVerifier.verify(mcpServer("ftp://example.com/mcp")));

        Assert.assertTrue(exception.getMessage().contains("HTTP or HTTPS"));
    }

    @Test
    public void verifyRejectsInvalidUrl() {
        IllegalArgumentException exception = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> AssistantMcpServerVerifier.verify(mcpServer("http://[::1")));

        Assert.assertTrue(exception.getMessage().contains("valid MCP server URL"));
    }

    @Test
    public void verifyRejectsMissingHost() {
        IllegalArgumentException exception = Assert.assertThrows(
                IllegalArgumentException.class,
                () -> AssistantMcpServerVerifier.verify(mcpServer("https:///mcp")));

        Assert.assertTrue(exception.getMessage().contains("valid host"));
    }

    private static AssistantMcpServer mcpServer(String url) {
        AssistantMcpServer mcpServer = new AssistantMcpServer(EntityId.create(true));
        mcpServer.setType(AssistantMcpServerType.HTTP);
        mcpServer.setName("Test MCP Server");
        mcpServer.setUrl(url);
        return mcpServer;
    }
}
