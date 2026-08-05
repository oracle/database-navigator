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

package com.dbn.mcp.action;

import com.dbn.common.util.Messages;
import com.dbn.mcp.registry.McpServerRecord;
import com.dbn.mcp.registry.McpServerRegistry;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.icon.Icons.ACTION_DELETE;
import static com.dbn.common.util.Messages.options;
import static com.dbn.nls.NlsResources.txt;

public class McpServerDeleteAction extends McpServerAction {
    public McpServerDeleteAction() {
        super(txt("msg.mcp.action.DeleteServer"), ACTION_DELETE);
    }

    @Override
    protected void actionPerformed(
            @NotNull AnActionEvent e,
            @NotNull Project project,
            @NotNull McpServerRecord record) {
        int option = Messages.showConfirmationDialog(
                project,
                txt("msg.mcp.title.DeleteMcpServer"),
                txt("msg.mcp.question.DeleteMcpServer", record.getServerName()),
                options(
                        txt("msg.mcp.button.DeleteServerOnly"),
                        txt("msg.mcp.button.DeleteServerAndFiles"),
                        txt("msg.shared.button.Cancel")), 0);
        if (option == 0 || option == 1) {
            McpServerRegistry.getInstance(project).removeRecord(record, option == 1);
        }
    }
}
