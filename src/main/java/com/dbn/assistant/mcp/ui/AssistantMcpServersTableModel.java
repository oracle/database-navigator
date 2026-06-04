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

package com.dbn.assistant.mcp.ui;

import com.dbn.assistant.mcp.model.AssistantMcpServer;
import com.dbn.assistant.mcp.model.AssistantMcpServerBundle;
import com.dbn.assistant.mcp.model.AssistantMcpServerType;
import com.dbn.common.ui.table.DBNEntityEditableTableModel;
import com.dbn.common.util.Strings;
import com.intellij.openapi.options.ConfigurationException;

import static com.dbn.nls.NlsResources.txt;

public class AssistantMcpServersTableModel extends DBNEntityEditableTableModel<AssistantMcpServer> {

    AssistantMcpServersTableModel(AssistantMcpServerBundle mcpServers) {
        super(() -> mcpServers.getElements());

        addColumn(txt("app.assistant.column.ServerName"), String.class, c -> c.getName(), (c, v) -> c.setName(v));
        addColumn(txt("app.assistant.column.ServerType"), AssistantMcpServerType.class, c -> c.getType(), null);
        addColumn(txt("app.assistant.column.UrlCommand"), String.class, c -> c.getEndpoint(), null);
    }


    public void validate() throws ConfigurationException {
        for (AssistantMcpServer mcpServer : getElements()) {
            if (Strings.isEmpty(mcpServer.getName())) {
                throw new ConfigurationException("Please provide names for all mcp servers.");
            }
        }
    }
}
