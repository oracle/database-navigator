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
import org.jdom.Element;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class AssistantMcpServerApprovalAdapterTest {
    private final AssistantMcpServerApprovalAdapter adapter = new AssistantMcpServerApprovalAdapter();

    @Test
    public void approvalKeyChangesWhenHttpUrlChanges() {
        AssistantMcpServer mcpServer = httpMcpServer("https://example.com/mcp");
        String approvalKey = adapter.getApprovalKey(mcpServer);

        mcpServer.setUrl("https://example.org/mcp");

        Assert.assertNotEquals(approvalKey, adapter.getApprovalKey(mcpServer));
    }

    @Test
    public void approvalKeyChangesWhenStdioCommandChanges() {
        AssistantMcpServer mcpServer = stdioMcpServer("/usr/local/bin/server", "--workspace", "/tmp/project-a");
        String approvalKey = adapter.getApprovalKey(mcpServer);

        mcpServer.setCommand("/usr/local/bin/other-server");

        Assert.assertNotEquals(approvalKey, adapter.getApprovalKey(mcpServer));
    }

    @Test
    public void approvalKeyChangesWhenStdioArgumentsChange() {
        AssistantMcpServer mcpServer = stdioMcpServer("/usr/local/bin/server", "--workspace", "/tmp/project-a");
        String approvalKey = adapter.getApprovalKey(mcpServer);

        mcpServer.setCommandArguments(List.of("--workspace", "/tmp/project-b"));

        Assert.assertNotEquals(approvalKey, adapter.getApprovalKey(mcpServer));
    }

    @Test
    public void importedProjectHttpServerIsNotAcknowledged() {
        AssistantMcpServer mcpServer = new AssistantMcpServer();
        mcpServer.readConfiguration(httpServerElement("https://example.com/mcp"));

        Assert.assertEquals("https://example.com/mcp", mcpServer.getEndpoint());
        Assert.assertTrue(adapter.getApprovalKey(mcpServer).contains("mcp-server:imported-http-server:"));
    }

    @Test
    public void importedProjectStdioServerIsNotAcknowledged() {
        AssistantMcpServer mcpServer = new AssistantMcpServer();
        mcpServer.readConfiguration(stdioServerElement("/usr/local/bin/server", "--workspace", "/tmp/project-a"));

        Assert.assertEquals("/usr/local/bin/server --workspace /tmp/project-a", mcpServer.getEndpoint());
        Assert.assertTrue(adapter.getApprovalKey(mcpServer).contains("mcp-server:imported-stdio-server:"));
    }

    private static AssistantMcpServer httpMcpServer(String url) {
        AssistantMcpServer mcpServer = mcpServer();
        mcpServer.setType(AssistantMcpServerType.HTTP);
        mcpServer.setUrl(url);
        return mcpServer;
    }

    private static AssistantMcpServer stdioMcpServer(String command, String... arguments) {
        AssistantMcpServer mcpServer = mcpServer();
        mcpServer.setType(AssistantMcpServerType.STDIO);
        mcpServer.setCommand(command);
        mcpServer.setCommandArguments(List.of(arguments));
        return mcpServer;
    }

    private static AssistantMcpServer mcpServer() {
        AssistantMcpServer mcpServer = new AssistantMcpServer(EntityId.get("test-mcp-server"));
        mcpServer.setName("Test MCP Server");
        mcpServer.setKey("usr_mcp0");
        return mcpServer;
    }

    private static Element httpServerElement(String url) {
        Element element = serverElement("imported-http-server", AssistantMcpServerType.HTTP);
        element.setAttribute("url", url);
        return element;
    }

    private static Element stdioServerElement(String command, String... arguments) {
        Element element = serverElement("imported-stdio-server", AssistantMcpServerType.STDIO);
        element.setAttribute("command", command);
        for (String argument : arguments) {
            Element argumentElement = new Element("command-argument");
            argumentElement.setText(argument);
            element.addContent(argumentElement);
        }
        return element;
    }

    private static Element serverElement(String id, AssistantMcpServerType type) {
        Element element = new Element("mcp-server");
        element.setAttribute("id", id);
        element.setAttribute("type", type.name());
        element.setAttribute("name", "Imported MCP Server");
        element.setAttribute("key", "usr_mcp0");
        return element;
    }
}
