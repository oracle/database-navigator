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

import com.dbn.assistant.AssistantPrerequisiteManager;
import com.dbn.assistant.provider.ProviderType;
import com.dbn.assistant.service.DatabaseService;
import com.dbn.common.color.Colors;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.intellij.ide.BrowserUtil;
import com.intellij.ui.HyperlinkLabel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import static com.dbn.common.util.Conditional.when;

/**
 * Database Assistant prerequisites information form
 * Explains the necessary grants and access rights for Select AI.
 * Also provisions actions to grant such privileges
 * TODO FEATURE proposal: "Grant for colleague" allowing the user to select another user for the grant operation
 *
 * @author Ayoub Aarrasse (Oracle)
 * @author Dan Cioca (Oracle)
 */
public class AssistantHelpForm extends DBNFormBase {

  private JPanel mainPanel;
  private JLabel intro;
  private JLabel networkAllow;
  private JComboBox<ProviderType> providerComboBox;
  private JTextArea aclTextArea;
  private JTextArea grantTextArea;
  private JLabel grantTextField;
  private JButton copyACLButton;
  private JButton applyACLButton;
  private JButton copyPrivilegeButton;
  private JPanel headerPanel;
  private HyperlinkLabel docuLink;
  private final String SELECT_AI_DOCS = "https://docs.oracle.com/en-us/iaas/autonomous-database-serverless/doc/sql-generation-ai-autonomous.html";

  private final ConnectionRef connection;
  private final DatabaseService databaseSvc;

  // Pass Project object to constructor
  public AssistantHelpForm(AssistantHelpDialog dialog) {
    super(dialog);

    ConnectionHandler connection = dialog.getConnection();
    this.connection = ConnectionRef.of(connection);
    this.databaseSvc = DatabaseService.getInstance(connection);

    initHeaderPanel();
    initializeWindow();
  }

  private ConnectionHandler getConnection() {
    return connection.ensure();
  }

  private void initHeaderPanel() {
    ConnectionHandler connection = getConnection();
    DBNHeaderForm headerForm = new DBNHeaderForm(this, connection);
    headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  private void initializeWindow() {
    ProviderType.values().forEach(p -> providerComboBox.addItem(p));

    docuLink.addHyperlinkListener(e -> when(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED, () -> BrowserUtil.browse(SELECT_AI_DOCS)));
    docuLink.setHyperlinkText("Select AI Docs");

    Color background = Colors.lafBrighter(Colors.getEditorBackground(), 5);

    String userName = getConnection().getUserName();
    grantTextField.setText(txt("permissions3.message", userName));
    grantTextArea.setText(txt("permissions4.message", userName));
    grantTextArea.setBackground(background);

    networkAllow.setText(txt("permissions5.message"));
    aclTextArea.setText(txt("permissions6.message", getAccessPoint(), userName));
    aclTextArea.setBackground(background);

    providerComboBox.addActionListener(e -> aclTextArea.setText(txt("permissions6.message", getAccessPoint(), userName)));

    copyPrivilegeButton.addActionListener(e -> copyTextToClipboard(grantTextArea.getText()));
    copyACLButton.addActionListener(e -> copyTextToClipboard(aclTextArea.getText()));

    applyACLButton.addActionListener(e -> grantNetworkAccess());
  }

  private void grantNetworkAccess() {
    ConnectionHandler connection = getConnection();
    AssistantPrerequisiteManager prerequisiteManager = getPrerequisiteManager();
    prerequisiteManager.grantNetworkAccess(connection, getSelectedProvider(), aclTextArea.getText());
  }

  private void grantExecutionPrivileges() {
    AssistantPrerequisiteManager prerequisiteManager = getPrerequisiteManager();
    ConnectionHandler connection = getConnection();
    prerequisiteManager.grantExecutionPrivileges(connection, connection.getUserName());
  }

  @NotNull
  private AssistantPrerequisiteManager getPrerequisiteManager() {
    return AssistantPrerequisiteManager.getInstance(ensureProject());
  }

  private String getAccessPoint() {
    ProviderType selectedProvider = getSelectedProvider();
    return selectedProvider == null ? "" : selectedProvider.getHost();
  }

  @Nullable
  private ProviderType getSelectedProvider() {
    return (ProviderType) providerComboBox.getSelectedItem();
  }

  private void copyTextToClipboard(String text) {
    StringSelection selection = new StringSelection(text);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(selection, null);
  }
}
