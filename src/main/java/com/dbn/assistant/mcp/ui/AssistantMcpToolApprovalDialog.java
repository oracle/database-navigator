/*
 * Copyright 2025 Oracle and/or its affiliates
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

import com.dbn.assistant.mcp.AssistantMcpServer;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.help.HelpTopic;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.help.HelpTopic.DATABASE_ASSISTANT_TOOLS;

/**
 * Wrapper factory result dialog
 * Lists all the database objects that were created as part of execution wrapper factory activity
 *
 * @author Dan Cioca (Oracle)
 */
public class AssistantMcpToolApprovalDialog extends DBNDialog<AssistantMcpToolApprovalsForm> {
  private final AssistantMcpServer mcpServer;

  public AssistantMcpToolApprovalDialog(Project project, AssistantMcpServer mcpServer) {
    super(project, "Assistant MCP Tool Approvals", false);
    this.setDefaultSize(640,  800);
    this.setModal(true);
    this.setAutoSize(true);
    this.mcpServer = mcpServer;
    init();
  }

  @Override
  protected HelpTopic getHelpTopic() {
    return DATABASE_ASSISTANT_TOOLS;
  }

  @NotNull
  @Override
  protected Action[] initializeActions() {
    renameAction(getCancelAction(), "Close");
    return actions(getCancelAction());
  }

  @Override
  protected @NotNull AssistantMcpToolApprovalsForm createForm() {
    return new AssistantMcpToolApprovalsForm(this, mcpServer);
  }
}
