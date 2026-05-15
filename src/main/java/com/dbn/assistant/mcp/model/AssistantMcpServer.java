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

package com.dbn.assistant.mcp.model;

import com.dbn.common.EntityId;
import com.dbn.common.acknowledgement.UserAcknowledgeable;
import com.dbn.common.checksum.Checksum;
import com.dbn.common.options.PersistentConfiguration;
import com.dbn.common.ui.Presentable;
import com.dbn.common.util.Cloneable;
import com.dbn.common.util.Strings;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.dbn.assistant.mcp.model.AssistantMcpServerType.HTTP;
import static com.dbn.common.checksum.ChecksumType.SHA_256;
import static com.dbn.common.options.setting.Settings.constantAttribute;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.setConstantAttribute;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Naming.nextNumberedIdentifier;
import static com.dbn.common.util.Strings.concatenate;
import static com.dbn.common.util.Unsafe.cast;

@Getter
@Setter
@NoArgsConstructor
public class AssistantMcpServer implements PersistentConfiguration, Presentable, Cloneable<AssistantMcpServer>, UserAcknowledgeable {
    public static final EntityId IDE_MCP_SERVER_ID = EntityId.get("ide-mcp-server-id");
    private static final Set<String> serverKeyStore = new HashSet<>();

    private AssistantMcpServerType type = AssistantMcpServerType.HTTP;
    private EntityId id;
    private String name;
    private String key;
    private String url;
    private String command;
    private String commandArguments;

    public AssistantMcpServer(EntityId id) {
        this.id = id;
        if (id.equals(IDE_MCP_SERVER_ID)) {
            this.key = "ide_mcp";
            serverKeyStore.add(this.key);
        }
    }

    public String getEndpoint() {
        return switch (type) {
            case HTTP -> url;
            case STDIO -> concatenate(getCommandTokens(), " ");
        };
    }

    @Override
    public String getAcknowledgementTitle() {
        return "Trust MCP Server \"" + getName() + "\"";
    }

    @Override
    public String getAcknowledgementMessage() {
        return "DB Assistant wants to use MCP server \"" + getName() + "\".\n\n" +
                "Endpoint type: " + getType().name() + "\n" +
                "Endpoint: " + getEndpoint() + "\n\n" +
                "Only acknowledge this endpoint if you trust this project configuration.";
    }

    @Override
    public String getAcknowledgementKey() {
        return "mcp-server:" + getId().id() + ":" + getEndpointFingerprint();
    }

    private String getEndpointFingerprint() {
        return Checksum.fromStringContent(getType().name() + ":" + getEndpoint(), SHA_256);
    }

    public List<String> getCommandTokens() {
        if (Strings.isEmpty(command)) return Collections.emptyList();

        ArrayList<String> tokens = new ArrayList<>();
        tokens.add(command);
        if (!Strings.isEmpty(commandArguments)) {
            tokens.addAll(List.of(commandArguments.split("\\s+")));
        }
        return tokens;
    }

    private static synchronized String registerKey(String serverKey) {
        if (serverKeyStore.contains(serverKey)) return serverKey;

        if (serverKey != null && serverKey.matches("usr_mcp[0-9]+")) {
            serverKeyStore.add(serverKey);
            return serverKey;
        }

        serverKey = nextNumberedIdentifier("usr_mcp0", false, () -> serverKeyStore);
        serverKeyStore.add(serverKey);
        return serverKey;
    }

    public String getKey() {
        key = registerKey(key);
        return key;
    }

    public boolean matchesUtilityName(String utilityName) {
        return utilityName.startsWith(key + "_");
    }

    public static String qualifiedUtilityName(String serverKey, String utilityName) {
        return serverKey + "_" + utilityName;
    }

    public String unqualifiedUtilityName(String utilityName) {
        if (matchesUtilityName(utilityName)) {
            return utilityName.substring(key.length() + 1);
        }
        return utilityName;
    }

    @Override
    public void readConfiguration(Element element) {
        id = constantAttribute(element, "id", EntityId.class);

        type = enumAttribute(element, "type", AssistantMcpServerType.class);
        name = stringAttribute(element, "name");
        key = stringAttribute(element, "key");
        url = stringAttribute(element, "url");
        command = stringAttribute(element, "command");
        commandArguments = stringAttribute(element, "command-arguments");
    }

    @Override
    public void writeConfiguration(Element element) {
        setConstantAttribute(element, "id", id);

        setEnumAttribute(element, "type", type);
        setStringAttribute(element, "name", name);
        setStringAttribute(element, "key", key);
        setStringAttribute(element, "url", url);
        setStringAttribute(element, "command", command);
        setStringAttribute(element, "command-arguments", commandArguments);
    }

    @Override
    @SneakyThrows
    public AssistantMcpServer clone() {
        return cast(super.clone());
    }

    public boolean isIdeMcpServer() {
        return id.equals(IDE_MCP_SERVER_ID);
    }
}
