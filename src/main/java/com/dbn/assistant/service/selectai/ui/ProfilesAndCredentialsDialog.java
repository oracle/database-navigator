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

package com.dbn.assistant.service.selectai.ui;

import com.dbn.assistant.service.selectai.credential.ui.CredentialManagementForm;
import com.dbn.assistant.service.selectai.profile.ui.ProfileManagementForm;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.help.HelpTopic;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;

/**
 * Main Database Assistant settings dialog
 * Features profiles and credential visualisation and management
 *
 * @author Dan Cioca (Oracle)
 */
public class ProfilesAndCredentialsDialog extends DBNDialog<ProfilesAndCredentialsForm> {

  private final ConnectionRef connection;

  public ProfilesAndCredentialsDialog(ConnectionHandler connection) {
    super(connection.getProject(), "Select AI Profiles and Credentials", true);
    this.connection = ConnectionRef.of(connection);
    setDefaultSize(800, 600);
    init();
  }

  @NotNull
  @Override
  protected Action[] initializeActions() {
    return actions(
            new HelpAction(),
            getCancelAction());
  }

  @Override
  protected HelpTopic getHelpTopic() {
    DBNForm configForm = getForm().getSelectedConfigForm();
    if (configForm instanceof CredentialManagementForm) return HelpTopic.DATABASE_ASSISTANT_CREDENTIALS;
    if (configForm instanceof ProfileManagementForm) return HelpTopic.DATABASE_ASSISTANT_AI_PROFILES;
    return HelpTopic.DATABASE_ASSISTANT_SELECT_AI;
  }

  @Deprecated // TODO use standard help mechanism
  private class HelpAction extends AbstractAction {
    private HelpAction() {
      super("Help");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      Dialogs.show(() -> new SelectAiHelpDialog(getConnection()));
    }
  }

  private ConnectionHandler getConnection() {
    return connection.ensure();
  }

  @Override
  protected @NotNull ProfilesAndCredentialsForm createForm() {
    return new ProfilesAndCredentialsForm(this, getConnection());
  }
}
