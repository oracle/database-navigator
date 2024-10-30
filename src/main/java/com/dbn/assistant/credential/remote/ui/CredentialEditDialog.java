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

package com.dbn.assistant.credential.remote.ui;

import com.dbn.common.outcome.DialogCloseOutcomeHandler;
import com.dbn.common.outcome.OutcomeHandler;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.object.DBCredential;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBCredentialType;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Set;

public class CredentialEditDialog extends DBNDialog<CredentialEditForm> {

  private final ConnectionRef connection;
  private final DBObjectRef<DBCredential> credential;
  private final Set<String> usedCredentialNames;
  private CredentialEditFormValidator validator;


  public CredentialEditDialog(ConnectionHandler connection, @Nullable DBCredential credential, @NotNull Set<String> usedCredentialNames) {
    super(connection.getProject(), getDialogTitle(credential), true);
    this.connection = ConnectionRef.of(connection);
    this.credential = DBObjectRef.of(credential);
    this.usedCredentialNames = usedCredentialNames;

    init();
  }

  private static String getDialogTitle(@Nullable DBCredential credential) {
    return credential == null ? "Create Credential" : "Update Credential";
  }

  /**
   * Defines the validation logic for the fields
   */
  @Override
  protected ValidationInfo doValidate() {
    return validator.validate();
  }

  /**
   * Defines the behaviour when we click the create/update button
   * It starts by validating, and then it executes the specifies action
   */
  @Override
  protected void doOKAction() {
    CredentialEditForm form = getForm();
    if (form.getSaveLocalCheckBox().isSelected() && form.getCredentialTypeComboBox().getSelectedItem() == DBCredentialType.PASSWORD) {
      form.saveProviderInfo();
    }
    if (credential != null) {
      form.doUpdateAction(dialogClose());
    } else {
      form.doCreateAction(dialogClose());
    }

  }

  private OutcomeHandler dialogClose() {
    return DialogCloseOutcomeHandler.create(this);
  }

  @Override
  protected Action @NotNull [] createActions() {
    super.setOKButtonText(txt(credential != null ? "ai.messages.button.update" : "ai.messages.button.create"));
    return super.createActions();
  }

  public ConnectionHandler getConnection() {
    return connection.ensure();
  }
  @Override
  protected @NotNull CredentialEditForm createForm() {
    CredentialEditForm form = new CredentialEditForm(this, DBObjectRef.get(credential), usedCredentialNames);
    validator = new CredentialEditFormValidator(form);
    return form;
  }


}
