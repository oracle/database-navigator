/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * This software is dual-licensed to you under the Universal Permissive License
 * (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl or Apache License
 * 2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
 * either license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.dbn.assistant.settings.ui;

import com.dbn.assistant.DatabaseAssistantManager;
import com.dbn.assistant.help.ui.AssistantHelpDialog;
import com.dbn.assistant.state.AssistantState;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Main Database Assistant settings dialog
 * Features profiles and credential visualisation and management
 *
 * @author Dan Cioca (Oracle)
 */
public class AssistantDatabaseConfigDialog extends DBNDialog<AssistantDatabaseConfigForm> {

  private final ConnectionRef connection;

  public AssistantDatabaseConfigDialog(ConnectionHandler connection) {
    super(connection.getProject(), getAssistantName(connection) + " Profiles and Credentials", true);
    this.connection = ConnectionRef.of(connection);
    renameAction(getCancelAction(), "Close");

    setDefaultSize(800, 600);
    init();
  }

  private static String getAssistantName(ConnectionHandler connection) {
    Project project = connection.getProject();
    DatabaseAssistantManager assistantManager = DatabaseAssistantManager.getInstance(project);
    AssistantState assistantState = assistantManager.getAssistantState(connection.getConnectionId());
    return assistantState.getAssistantName();
  }

  @Override
  protected Action @NotNull [] createActions() {
    return new Action[]{getCancelAction(), new HelpAction()};
  }

  private class HelpAction extends AbstractAction {
    private HelpAction() {
      super("Help");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      Dialogs.show(() -> new AssistantHelpDialog(getConnection()));
    }
  }

  private ConnectionHandler getConnection() {
    return connection.ensure();
  }

  @Override
  protected @NotNull AssistantDatabaseConfigForm createForm() {
    return new AssistantDatabaseConfigForm(this, getConnection());
  }
}
