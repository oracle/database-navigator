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
import com.dbn.common.util.Sockets;
import lombok.experimental.UtilityClass;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.nls.NlsResources.txt;

@UtilityClass
public class AssistantMcpServerVerifier {

    public static void verify(AssistantMcpServer mcpServer) {
        switch (mcpServer.getType()) {
            case HTTP -> verifyHttpMcpServerUrl(mcpServer);
            case STDIO -> {}
        }
    }

    private static void verifyHttpMcpServerUrl(AssistantMcpServer mcpServer) {
        if (mcpServer.isIdeMcpServer()) return;

        String url = mcpServer.getUrl();
        if (isEmpty(url)) {
            throw new IllegalArgumentException(txt("msg.assistant.error.McpServerUrlRequired"));
        }

        URI uri = parseHttpMcpServerUri(url);

        String host = uri.getHost();
        if (isEmpty(host)) {
            throw new IllegalArgumentException(txt("msg.assistant.error.McpServerUrlHostRequired"));
        }

        String scheme = uri.getScheme();
        if ("https".equalsIgnoreCase(scheme)) return;

        if ("http".equalsIgnoreCase(scheme)) {
            if (isLocalNetworkHost(host)) return;

            throw new IllegalArgumentException(txt("msg.assistant.error.McpServerRemoteUrlRequiresHttps"));
        }

        throw new IllegalArgumentException(txt("msg.assistant.error.McpServerUrlSchemeRequired"));
    }

    private static URI parseHttpMcpServerUri(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(txt("msg.assistant.error.McpServerUrlInvalid"), e);
        }
    }

    private static boolean isLocalNetworkHost(String host) {
        try {
            return Sockets.isLocalNetworkHost(host);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(txt("msg.assistant.error.McpServerUrlHostUnresolved", host), e);
        }
    }
}
