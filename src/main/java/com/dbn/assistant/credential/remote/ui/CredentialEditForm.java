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

import com.dbn.assistant.credential.local.LocalCredential;
import com.dbn.assistant.credential.local.LocalCredentialSettings;
import com.dbn.assistant.settings.AssistantSettings;
import com.dbn.common.exception.Exceptions;
import com.dbn.common.outcome.OutcomeHandler;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.TextFields;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.object.DBCredential;
import com.dbn.object.DBSchema;
import com.dbn.object.impl.DBCredentialImpl;
import com.dbn.object.management.ObjectManagementService;
import com.dbn.object.type.DBCredentialType;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.Objects;
import java.util.Set;

import static com.dbn.common.ui.CardLayouts.showCard;
import static com.dbn.object.type.DBAttributeType.FINGERPRINT;
import static com.dbn.object.type.DBAttributeType.PASSWORD;
import static com.dbn.object.type.DBAttributeType.PRIVATE_KEY;
import static com.dbn.object.type.DBAttributeType.USER_NAME;
import static com.dbn.object.type.DBAttributeType.USER_OCID;
import static com.dbn.object.type.DBAttributeType.USER_TENANCY_OCID;

/**
 * A dialog window for creating new AI credentials.
 * This window allows users to input credential information, supporting different types of credentials.
 * It interacts with {@link CredentialManagementService} to create credentials in the system.
 */
@Getter
public class CredentialEditForm extends DBNFormBase {

  private JPanel mainPanel;
  private JTextField credentialNameField;
  private JComboBox<DBCredentialType> credentialTypeComboBox;
  private JTextField passwordCredentialUsernameField;
  private javax.swing.JPasswordField passwordCredentialPasswordField;
  private JPanel attributesPane;
  private JTextField ociCredentialUserOcidField;
  private JTextField ociCredentialUserTenancyOcidField;
  private JTextField ociCredentialPrivateKeyField;
  private JTextField ociCredentialFingerprintField;
  private JButton localCredentialPickerButton;
  private JCheckBox saveLocalCheckBox;
  private JCheckBox statusCheckBox;
  private JPanel passwordCard;
  private JPanel ociCard;
  private JLabel errorLabel;


  private final ConnectionRef connection;
  private DBCredential credential;
  private LocalCredential localCredential;
  private final Set<String> usedCredentialNames;

  /**
   * Constructs a CredentialEditForm
   *
   * @param dialog the parent dialog
   * @param credential the credential to be edited, can be null in case of credential creation
   * @param usedCredentialNames the names of credentials which are already defined and name can no longer be used
   */
  public CredentialEditForm(CredentialEditDialog dialog, @Nullable DBCredential credential, Set<String> usedCredentialNames) {
    super(dialog);
    this.connection = dialog.getConnection().ref();
    this.credential = credential;
    this.usedCredentialNames = usedCredentialNames;

    initCredentialTypeComboBox();
    initCredentialPickerButton();
    initCredentialAttributeFields();
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  private ConnectionHandler getConnection() {
    return connection.ensure();
  }

  private void initCredentialTypeComboBox() {
    credentialTypeComboBox.addItem(DBCredentialType.PASSWORD);
    credentialTypeComboBox.addItem(DBCredentialType.OCI);
    credentialTypeComboBox.addActionListener((e) -> showCard(attributesPane, credentialTypeComboBox.getSelectedItem()));
    credentialTypeComboBox.setEnabled(credential == null);
  }

  private void initCredentialPickerButton() {
    localCredentialPickerButton.addActionListener((e) -> showCredentialPicker());
  }

  private void showCredentialPicker() {
    Dialogs.show(() -> new CredentialPickerDialog(getProject(), c -> pickLocalCredential(c)));
  }

  private void pickLocalCredential(LocalCredential credential) {
    localCredential = credential;
    passwordCredentialUsernameField.setText(credential.getUser());
    passwordCredentialPasswordField.setText(credential.getKey());
    saveLocalCheckBox.setEnabled(false);
  }

  /**
   * Populate fields with the attributes of the credential to be updated
   */
  private void initCredentialAttributeFields() {
    if (credential == null) return;

    credentialNameField.setText(credential.getName());
    credentialNameField.setEnabled(false);
    statusCheckBox.setSelected(credential.isEnabled());
    DBCredentialType credentialType = credential.getType();
    if (credentialType == DBCredentialType.PASSWORD) {
      initPasswordCredentialFields();
    } else if (credentialType == DBCredentialType.OCI) {
      initOciCredentialFields();
    }
  }

  private void initOciCredentialFields() {
    credentialTypeComboBox.setSelectedItem(DBCredentialType.OCI);
    ociCredentialUserOcidField.setText(credential.getUserName());
  }

  private void initPasswordCredentialFields() {
    credentialTypeComboBox.setSelectedItem(DBCredentialType.PASSWORD);
    passwordCredentialUsernameField.setText(credential.getUserName());
    TextFields.onTextChange(passwordCredentialUsernameField, e -> initLocalSaveCheckbox());
    TextFields.onTextChange(passwordCredentialPasswordField, e -> initLocalSaveCheckbox());
  }

  private void initLocalSaveCheckbox() {
    boolean enabled = canSaveLocalCredential();
    boolean selected = enabled && saveLocalCheckBox.isSelected();
    saveLocalCheckBox.setEnabled(enabled);
    saveLocalCheckBox.setSelected(selected);
  }

  private boolean canSaveLocalCredential() {
    if (localCredential == null) return true; // local credential has been chosen to fill the attributes
    return
        !Objects.equals(localCredential.getUser(), passwordCredentialUsernameField.getText()) ||
        !Objects.equals(localCredential.getKey(), passwordCredentialPasswordField.getText());
  }


  /**
   * Collects the fields' info and sends them to the service layer to create new credential
   */
  protected void doCreateAction(OutcomeHandler successHandler) {
    credential = inputsToCredential();
    if (credential == null) return;
    getManagementService().createObject(credential, successHandler);
  }

  /**
   * Collects the fields' info and sends them to the service layer to update new credential
   */
  protected void doUpdateAction(OutcomeHandler successHandler) {
    credential = inputsToCredential();
    if (credential == null) return;
    getManagementService().updateObject(credential, successHandler);
  }

  @NotNull
  private ObjectManagementService getManagementService() {
    return ObjectManagementService.getInstance(ensureProject());
  }

  private Void handleException(Throwable e) {
    Dispatch.run(mainPanel, () -> Messages.showErrorDialog(getProject(), Exceptions.causeMessage(e)));
    return null;
  }

  @Nullable
  @SneakyThrows
  private DBCredential inputsToCredential() {
    DBCredentialType credentialType = (DBCredentialType) credentialTypeComboBox.getSelectedItem();
    if (credentialType == null) return null;

    DBSchema schema = getConnection().getObjectBundle().getUserSchema();
    String credentialName = credentialNameField.getText();
    boolean selected = statusCheckBox.isSelected();

    DBCredential credential = new DBCredentialImpl(schema, credentialName, credentialType, selected);
    if (credentialType == DBCredentialType.PASSWORD) {
      credential.setAttribute(USER_NAME, passwordCredentialUsernameField.getText());
      credential.setAttribute(PASSWORD, passwordCredentialPasswordField.getText());

    } else if (credentialType == DBCredentialType.OCI) {
      credential.setAttribute(USER_OCID,         ociCredentialUserOcidField.getText());
      credential.setAttribute(USER_TENANCY_OCID, ociCredentialUserTenancyOcidField.getText());
      credential.setAttribute(PRIVATE_KEY,       ociCredentialPrivateKeyField.getText());
      credential.setAttribute(FINGERPRINT,       ociCredentialFingerprintField.getText());

    }
    return credential;
  }

  protected void saveProviderInfo() {
    Project project = ensureProject();
    LocalCredentialSettings settings = AssistantSettings.getInstance(project).getCredentialSettings();
    LocalCredential credential = new LocalCredential();
    credential.setName(credentialNameField.getText());
    credential.setUser(passwordCredentialUsernameField.getText());
    credential.setKey(passwordCredentialPasswordField.getText());
    settings.getCredentials().add(credential);
  }
}
