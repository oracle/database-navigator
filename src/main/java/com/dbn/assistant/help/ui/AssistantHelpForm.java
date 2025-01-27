/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.assistant.help.ui;

import com.dbn.assistant.AssistantPrerequisiteManager;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.common.color.Colors;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.intellij.ui.HyperlinkLabel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

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
  private JComboBox<AIProvider> providerComboBox;
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

  // Pass Project object to constructor
  public AssistantHelpForm(AssistantHelpDialog dialog) {
    super(dialog);

    ConnectionHandler connection = dialog.getConnection();
    this.connection = ConnectionRef.of(connection);

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
    AIProvider.values().forEach(p -> providerComboBox.addItem(p));

    docuLink.setHyperlinkText("Select AI Docs");
    docuLink.setHyperlinkTarget(SELECT_AI_DOCS);

    Color background = Colors.lafBrighter(Colors.getEditorBackground(), 5);

    String userName = getConnection().getUserName();
    grantTextField.setText(txt("cfg.assistant.text.GrantExecution", userName));
    grantTextArea.setText(txt("cfg.assistant.code.GrantExecution", userName));
    grantTextArea.setBackground(background);

    networkAllow.setText(txt("cfg.assistant.text.AllowNetworkAccess"));
    aclTextArea.setText(txt("cfg.assistant.code.AllowNetworkAccess", getAccessPoint(), userName));
    aclTextArea.setBackground(background);

    providerComboBox.addActionListener(e -> aclTextArea.setText(txt("cfg.assistant.code.AllowNetworkAccess", getAccessPoint(), userName)));

    copyPrivilegeButton.addActionListener(e -> copyTextToClipboard(grantTextArea.getText()));
    copyACLButton.addActionListener(e -> copyTextToClipboard(aclTextArea.getText()));

    applyACLButton.addActionListener(e -> grantNetworkAccess());
  }

  private void grantNetworkAccess() {
    AIProvider selectedProvider = getSelectedProvider();
    if (selectedProvider == null) return;

    ConnectionHandler connection = getConnection();
    AssistantPrerequisiteManager prerequisiteManager = getPrerequisiteManager();
    prerequisiteManager.grantNetworkAccess(connection, selectedProvider, aclTextArea.getText());
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
    AIProvider selectedProvider = getSelectedProvider();
    return selectedProvider == null ? "" : selectedProvider.getHost();
  }

  @Nullable
  private AIProvider getSelectedProvider() {
    return (AIProvider) providerComboBox.getSelectedItem();
  }

  private void copyTextToClipboard(String text) {
    StringSelection selection = new StringSelection(text);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(selection, null);
  }
}
