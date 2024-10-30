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

package com.dbn.assistant.help.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class AssistantHelpDialog extends DBNDialog<AssistantHelpForm> {

  private final ConnectionRef connection;

  public AssistantHelpDialog(ConnectionHandler connection) {
    super(connection.getProject(), "Select AI Help", true);
    this.connection = ConnectionRef.of(connection);
    renameAction(getCancelAction(), "Close");

    setResizable(false);
    init();
  }

  @Override
  protected Action @NotNull [] createActions() {
    return new Action[]{getCancelAction()};
  }

  public ConnectionHandler getConnection() {
    return connection.ensure();
  }

  @Override
  protected @NotNull AssistantHelpForm createForm() {
    return new AssistantHelpForm(this);
  }
}
