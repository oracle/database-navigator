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

package com.dbn.assistant.profile.wizard;

import com.dbn.assistant.credential.remote.ui.CredentialEditDialog;
import com.dbn.assistant.profile.wizard.validation.ProfileCredentialVerifier;
import com.dbn.assistant.profile.wizard.validation.ProfileNameVerifier;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Background;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Lists;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.object.DBCredential;
import com.dbn.object.DBSchema;
import com.dbn.object.event.ObjectChangeListener;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import org.jetbrains.annotations.Nullable;

import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.Set;

import static com.dbn.common.util.Commons.nvln;
import static com.dbn.common.util.Lists.convert;
import static com.dbn.nls.NlsResources.txt;

/**
 * Profile edition general step for edition wizard.
 *
 * @see ProfileEditionWizard
 */
public class ProfileEditionGeneralStep extends WizardStep<ProfileEditionWizardModel> implements Disposable {
  private JPanel mainPanel;
  private JTextField nameTextField;
  private JComboBox<String> credentialComboBox;
  private JTextField descriptionTextField;
  private JButton addCredentialButton;

  private final ConnectionRef connection;
  private final ProfileData profile;
  private final Set<String> existingProfileNames;

  private final boolean isUpdate;

  public ProfileEditionGeneralStep(ConnectionHandler connection, ProfileData profile, Set<String> existingProfileNames, boolean isUpdate) {
    super(txt("profile.mgmt.general_step.title"),
            txt("profile.mgmt.general_step.explaination"));
    this.connection = ConnectionRef.of(connection);
    this.profile = profile;
    this.existingProfileNames = existingProfileNames;
    this.isUpdate = isUpdate;

    initCredentialAddButton();
    initializeUI();
    addValidationListener();

    UserInterface.whenShown(mainPanel, () -> populateCredentials());
  }

  private void initCredentialAddButton() {
    addCredentialButton.setIcon(Icons.ACTION_ADD);
    addCredentialButton.setText(null);

    ConnectionHandler connection = getConnection();
    addCredentialButton.addActionListener(e -> Dialogs.show(() -> new CredentialEditDialog(connection, null, Set.of())));

    Project project = connection.getProject();
    ProjectEvents.subscribe(project, this, ObjectChangeListener.TOPIC, (connectionId, ownerId, objectType) -> {
      if (connectionId != connection.getConnectionId()) return;
      if (objectType != DBObjectType.CREDENTIAL) return;
      populateCredentials();
    });
  }

  ConnectionHandler getConnection() {
    return ConnectionRef.ensure(connection);
  }

  private void initializeUI() {
    if (isUpdate) {
      nameTextField.setText(profile.getName());
      descriptionTextField.setText(profile.getDescription());
      nameTextField.setEnabled(false);
      credentialComboBox.setEnabled(true);
      descriptionTextField.setEnabled(false);
    }
  }

  private void addValidationListener() {
    nameTextField.setInputVerifier(new ProfileNameVerifier(existingProfileNames, isUpdate));
    credentialComboBox.setInputVerifier(new ProfileCredentialVerifier());
    nameTextField.getDocument().addDocumentListener(new DocumentListener() {
      public void changedUpdate(DocumentEvent e) {
        nameTextField.getInputVerifier().verify(nameTextField);
      }

      public void removeUpdate(DocumentEvent e) {
        nameTextField.getInputVerifier().verify(nameTextField);
      }

      public void insertUpdate(DocumentEvent e) {
        nameTextField.getInputVerifier().verify(nameTextField);
      }
    });
    credentialComboBox.addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        InputVerifier verifier = credentialComboBox.getInputVerifier();
        if (verifier != null) {
          verifier.verify(credentialComboBox);
        }
      }
    });

  }

  private void populateCredentials() {
    ConnectionHandler connection = getConnection();
    Project project = connection.getProject();

    Background.run(project, () -> {
      String currentCredential = profile.getCredentialName();
      DBSchema schema = connection.getObjectBundle().getUserSchema();
      if (schema == null) return;

      List<DBCredential> credentials = schema.getCredentials();
      List<String> credentialNames = convert(credentials, c -> c.getName());
      if (currentCredential != null && !credentialNames.contains(currentCredential)) credentialNames.add(currentCredential);

      credentialComboBox.removeAllItems();
      credentialNames.forEach(c -> credentialComboBox.addItem(c));
      String selectedCredential = nvln(currentCredential, Lists.firstElement(credentialNames));
      credentialComboBox.setSelectedItem(selectedCredential);
    });

/*
    credentialSvc.list().thenAccept(credentialProviderList -> {
      SwingUtilities.invokeLater(() -> {


        credentialComboBox.removeAllItems();
        for (Credential credential : credentialProviderList) {
          credentialComboBox.addItem(credential.getName());
        }
        if (!credentialProviderList.isEmpty()) {
          credentialComboBox.setSelectedItem(currentCredential);
        }
      });
    });
*/
  }

  @Override
  public JComponent prepare(WizardNavigationState wizardNavigationState) {
    return mainPanel;
  }

  @Override
  public @Nullable JComponent getPreferredFocusedComponent() {
    return nameTextField;
  }

  @Override
  public WizardStep<ProfileEditionWizardModel> onNext(ProfileEditionWizardModel model) {
    boolean nameValid = isUpdate || nameTextField.getInputVerifier().verify(nameTextField);
    boolean credentialValid = credentialComboBox.getInputVerifier().verify(credentialComboBox);
    profile.setName(nameTextField.getText());
    profile.setCredentialName((String) credentialComboBox.getSelectedItem());
    // special case for description: null and empty string is the same
    //    do not confuse Profile.equals() because of that
    if (descriptionTextField.getText().isEmpty()) {
      // did the user really remove the description or was it missing
      // from the beginning ?
      if (profile.getDescription() != null && !profile.getDescription().isEmpty()) {
        profile.setDescription(descriptionTextField.getText());
      }
    } else {
      // set it in any case
      profile.setDescription(descriptionTextField.getText());
    }

    return nameValid && credentialValid ? super.onNext(model) : this;
  }

  @Override
  public void dispose() {
    // TODO dispose UI resources
  }
}
