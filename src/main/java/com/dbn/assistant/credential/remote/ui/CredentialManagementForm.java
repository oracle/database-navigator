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

import com.dbn.common.action.DataKeys;
import com.dbn.common.color.Colors;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.exception.Exceptions;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.CardLayouts;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Lists;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionRef;
import com.dbn.object.DBAIProfile;
import com.dbn.object.DBCredential;
import com.dbn.object.DBSchema;
import com.dbn.object.common.ui.DBObjectListCellRenderer;
import com.dbn.object.common.ui.DBObjectListModel;
import com.dbn.object.event.ObjectChangeListener;
import com.dbn.object.management.ObjectManagementService;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.util.ui.AsyncProcessIcon;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.util.Conditional.when;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.object.common.DBObjectUtil.refreshUserObjects;
import static com.dbn.object.type.DBObjectType.AI_PROFILE;
import static com.dbn.object.type.DBObjectType.CREDENTIAL;

/**
 * A panel for managing AI credentials within the application, offering functionalities
 * to view, edit, and delete AI credentials associated with a specific connection. This component
 * is part of the Oracle AI integration module, enabling users to manage their AI service
 * credentials directly within the IDE environment.
 * <p>
 * The panel dynamically populates with credential information retrieved from the AI credential service,
 * leveraging the {@link ConnectionHandler} to fetch and manage credentials for a given project connection.
 *
 * @author Ayoub Aarrasse (Oracle)
 * @author Emmanuel Jannetti (Oracle)
 */
public class CredentialManagementForm extends DBNFormBase {

  private JPanel mainPane;
  private JList<DBCredential> credentialList;
  private JPanel detailPanel;
  private JPanel actionsPanel;
  private JPanel initializingIconPanel;
  private JSplitPane contentSplitPane;

  /**
   * Keeps a mapping of profile names that used a specific credential name
   * (Assuming that credential names are unique within the DB)
   */
  private Map<String, Set<String>> credentialUsage = new HashMap<>();
  private Map<String, CredentialDetailsForm> credentialDetailForms = new ConcurrentHashMap<>();
  private final ConnectionRef connection;

  @Getter
  private boolean loading = false;
  /**
   * Initializes a new instance of the CredentialManagementPanel for managing AI credentials,
   * setting up UI components and fetching credentials for the given connection.
   *
   * @param connection The ConnectionHandler associated with this panel, used for fetching
   *                   and managing credentials related to the project's Oracle AI integration.
   */
  public CredentialManagementForm(DBNForm parent, @NotNull ConnectionHandler connection) {
    super(parent);
    this.connection = ConnectionRef.of(connection);

    initActionsPanel();
    initDetailsPanel();
    initCredentialList();
    initChangeListener();

    whenShown(() -> loadCredentials());
  }
  private void initChangeListener() {
    ProjectEvents.subscribe(ensureProject(), this, ObjectChangeListener.TOPIC, (connectionId, ownerId, objectType) -> {
      if (connectionId != getConnectionId()) return;
      if (objectType == CREDENTIAL) reloadCredentials();
      if (objectType == AI_PROFILE) evaluateCredentialUsage();
    });
  }

  @NotNull
  public ConnectionHandler getConnection() {
    return connection.ensure();
  }
  @Override
  protected JComponent getMainComponent() {
    return mainPane;
  }

  private void initActionsPanel() {
    ActionToolbar typeActions = Actions.createActionToolbar(actionsPanel, "DBNavigator.ActionGroup.AssistantCredentialManagement", "", true);
    this.actionsPanel.add(typeActions.getComponent(), BorderLayout.CENTER);
    initializingIconPanel.add(new AsyncProcessIcon("Loading"), BorderLayout.CENTER);
  }

  private void initDetailsPanel() {
    CardLayouts.addBlankCard(detailPanel);
  }

  /**
   * Initializes UI components of the panel, including setting up list selection listeners for credential selection,
   * configuring the appearance of the list and its cells, and initializing action listeners for add and delete buttons.
   * This method is responsible for the initial UI setup and layout of the credential management panel.
   */
  private void initCredentialList() {
    credentialList.setModel(DBObjectListModel.create(this));
    credentialList.setBackground(Colors.getTextFieldBackground());
    credentialList.setBorder(Borders.EMPTY_BORDER);

    // Configures credentialList with a list selection listener for updating display info based on selected credential
    credentialList.addListSelectionListener((e) -> {
      if (e.getValueIsAdjusting()) return;

      DBCredential selectedCredential = credentialList.getSelectedValue();
      showDetailForm(selectedCredential);
    });

    credentialList.setCellRenderer(DBObjectListCellRenderer.create());
  }

  public void promptCredentialCreation() {
    Dialogs.show(() -> new CredentialEditDialog(
            getConnection(), null,
            credentialUsage.keySet()));
  }

  public void promptCredentialEdition(@NotNull DBCredential credential) {
    Dialogs.show(() -> new CredentialEditDialog(
            getConnection(), credential,
            Collections.emptySet()));  // not relevant when editing an existing credential
  }

  public void promptCredentialDeletion(@NotNull DBCredential credential) {
    String credentialName = credential.getName();

    StringBuilder detailedMessage = new StringBuilder(txt("ai.settings.credential.deletion.message.prefix"));
    detailedMessage.append(' ');
    detailedMessage.append(credentialName);
    Set<String> uses = credentialUsage.get(credentialName);
    if (uses != null && !uses.isEmpty()) {
      detailedMessage.append('\n');
      detailedMessage.append(txt("ai.settings.credential.deletion.message.warning"));
      uses.forEach(c -> {
        detailedMessage.append(c);
        detailedMessage.append(", ");
      });
    }
    Messages.showQuestionDialog(getProject(),
            txt("ai.settings.credential.deletion.title"),
            detailedMessage.toString(),
            Messages.OPTIONS_YES_NO, 1,
            option -> when(option == 0, () -> removeCredential(credential)));
  }

  private boolean isCredentialUsed(DBCredential credential) {
    Set<String> usage = getCredentialUsage(credential.getName());
    return usage != null && !usage.isEmpty();
  }

  /**
   * Removes a specified credential by name and updates the local cache of credentials.
   *
   * @param credential The name of the credential to be removed.
   */
  private void removeCredential(DBCredential credential) {
    ObjectManagementService managementService = ObjectManagementService.getInstance(ensureProject());
    managementService.deleteObject(credential, null);
  }

  private @Nullable DBSchema getUserSchema() {
    return getConnection().getObjectBundle().getUserSchema();
  }

  /**
   * Asynchronously fetches the list of credential providers from the AI credential service and updates
   * the UI components accordingly. This method retrieves the credentials, updating the credential list
   * and the display information panel based on the available credentials for the connected project.
   */
  public void loadCredentials() {
    Background.run(getProject(), () -> doLoadCredentials(false));
  }

  public void reloadCredentials() {
    Background.run(getProject(), () -> doLoadCredentials(true));
  }

  private void doLoadCredentials(boolean force) {
    beforeLoad();
    try {
      if (force) refreshUserObjects(getConnectionId(), CREDENTIAL);
      DBSchema schema = getUserSchema();
      List<DBCredential> credentials =  schema == null ? Collections.emptyList() : schema.getCredentials();
      applyCredentials(credentials);

    } catch (Throwable e){
      handleLoadError(e);
    } finally {
      afterLoad();
    }
  }

  private ConnectionId getConnectionId() {
    return getConnection().getConnectionId();
  }

  private void handleLoadError(Throwable e) {
    conditionallyLog(e);
    Dispatch.run(mainPane, () -> Messages.showErrorDialog(getProject(), "Failed to load credentials.\nCause: " + Exceptions.causeMessage(e)));
    afterLoad();
  }

  private void applyCredentials(List<DBCredential> credentials) {
    // capture selection
    DBCredential selectedCredential = getSelectedCredential();
    String selectedCredentialName = selectedCredential == null ? null : selectedCredential.getName();

    // apply new credentials
    this.credentialDetailForms = Disposer.replace(
            this.credentialDetailForms,
            new ConcurrentHashMap<>());

    this.credentialList.setModel(Disposer.replace(
            credentialList.getModel(),
            DBObjectListModel.create(this, credentials)));

    evaluateCredentialUsage();

    // restore selection
    int selectionIndex = Lists.indexOf(credentials, c -> c.getName().equalsIgnoreCase(selectedCredentialName));
    if (selectionIndex == -1 && !credentials.isEmpty()) selectionIndex = 0;
    if (selectionIndex != -1) this.credentialList.setSelectedIndex(selectionIndex);
  }

  private void evaluateCredentialUsage() {
    DBSchema userSchema = getUserSchema();
    if (userSchema == null) return;

    credentialUsage.clear();
    List<DBAIProfile> profiles = userSchema.getAIProfiles();
    for (DBAIProfile profile : profiles) {
      String credentialName = profile.getCredentialName();
      Set<String> profileNames = getCredentialUsage(credentialName);
      profileNames.add(profile.getName());
    }
  }

  private void beforeLoad() {
    loading = true;
    freezeForm();

    initializingIconPanel.setVisible(true);
    credentialList.setBackground(Colors.getPanelBackground());
  }

  private void afterLoad() {
    loading = false;
    unfreezeForm();

    initializingIconPanel.setVisible(false);
    credentialList.setBackground(Colors.getTextFieldBackground());
    credentialList.revalidate();
    updateActionToolbars();
  }

  /**
   * Updates the display information panel based on a selected credential.
   * This method dynamically creates and displays UI components such as labels and text fields
   * to show detailed information for the selected credential, including its name and associated username.
   *
   * @param credential The credential to display information for.
   */
  public void showDetailForm(@Nullable DBCredential credential) {
    if (credential == null) {
      CardLayouts.showBlankCard(detailPanel);
    } else {
      String credentialName = credential.getName();
      credentialDetailForms.computeIfAbsent(credentialName, c -> createDetailForm(credential));
      CardLayouts.showCard(detailPanel, credentialName);
    }
  }

  @NotNull
  private CredentialDetailsForm createDetailForm(DBCredential credential) {
    CredentialDetailsForm detailsForm = new CredentialDetailsForm(this, credential);
    CardLayouts.addCard(detailPanel, detailsForm.getComponent(), credential.getName());
    return detailsForm;
  }


  @Nullable
  public DBCredential getSelectedCredential() {
    return credentialList.getSelectedValue();
  }

  @Nullable
  @Override
  public Object getData(@NotNull String dataId) {
    if (DataKeys.CREDENTIAL_MANAGEMENT_FORM.is(dataId)) return this;
    return null;
  }

  public Set<String> getCredentialUsage(String credentialName) {
    return credentialUsage.computeIfAbsent(credentialName, c -> new TreeSet<>());
  }
}
