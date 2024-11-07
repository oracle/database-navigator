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

package com.dbn.assistant.profile.ui;

import com.dbn.assistant.DatabaseAssistantManager;
import com.dbn.assistant.profile.wizard.ProfileEditionWizard;
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
import com.dbn.common.util.Lists;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionRef;
import com.dbn.object.DBAIProfile;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.dbn.common.util.Conditional.when;
import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.object.common.DBObjectUtil.refreshUserObjects;
import static com.dbn.object.type.DBObjectType.AI_PROFILE;

/**
 * Profile management bindings
 *
 * @author Ayoub Aarrasse (Oracle)
 * @author Emmanuel Jannetti (Oracle)
 */
public class ProfileManagementForm extends DBNFormBase {

  private JPanel mainPanel;
  private JList<DBAIProfile> profilesList;
  private JPanel detailPanel;
  private JPanel actionsPanel;
  private JPanel initializingIconPanel;
  private JSplitPane contentSplitPane;

  private final ConnectionRef connection;
  private final DatabaseAssistantManager manager;

  private Map<String, ProfileDetailsForm> profileDetailForms = new ConcurrentHashMap<>();

  @Getter
  private boolean loading;


  public ProfileManagementForm(DBNForm parent, @NotNull ConnectionHandler connection) {
    super(parent);
    this.connection = ConnectionRef.of(connection);
    this.manager = DatabaseAssistantManager.getInstance(connection.getProject());

    initActionsPanel();
    initProfilesList();
    initDetailsPanel();
    initChangeListener();

    whenShown(() -> loadProfiles());
  }

  private void initChangeListener() {
    ProjectEvents.subscribe(ensureProject(), this, ObjectChangeListener.TOPIC, (connectionId, ownerId, objectType) -> {
      if (connectionId != getConnection().getConnectionId()) return;
      if (objectType != AI_PROFILE) return;
      reloadProfiles();
    });
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  private void initProfilesList() {
    profilesList.setModel(DBObjectListModel.create(this));
    profilesList.setBackground(Colors.getTextFieldBackground());
    profilesList.setBorder(Borders.EMPTY_BORDER);

    profilesList.addListSelectionListener((e) -> {
      if (e.getValueIsAdjusting()) return;

      showDetailForm(getSelectedProfile());
    });

    profilesList.setCellRenderer(new ProfileListCellRenderer(getConnection()));
  }

  private void initActionsPanel() {
    ActionToolbar typeActions = Actions.createActionToolbar(actionsPanel, "DBNavigator.ActionGroup.AssistantProfileManagement", "", true);
    this.actionsPanel.add(typeActions.getComponent(), BorderLayout.CENTER);
    initializingIconPanel.add(new AsyncProcessIcon("Loading"), BorderLayout.CENTER);
  }

  private void initDetailsPanel() {
    CardLayouts.addBlankCard(detailPanel);
  }

  public void showDetailForm(DBAIProfile profile) {
    if (profile == null) {
      CardLayouts.showBlankCard(detailPanel);
    } else {
      String profileName = profile.getName();
      profileDetailForms.computeIfAbsent(profileName, c -> createDetailForm(profile));
      CardLayouts.showCard(detailPanel, profileName);
    }
  }

  @NotNull
  private ProfileDetailsForm createDetailForm(DBAIProfile profile) {
    ProfileDetailsForm detailsForm = new ProfileDetailsForm(this, profile);
    CardLayouts.addCard(detailPanel, detailsForm.getComponent(), profile.getName());
    return detailsForm;
  }

  public void promptProfileCreation() {
    ProfileEditionWizard.showWizard(getConnection(), null, getProfileNames(), null);
  }

  public void promptProfileEdition(@NotNull DBAIProfile profile) {
    ProfileEditionWizard.showWizard(getConnection(), profile, getProfileNames(), null);
  }

  public void promptProfileDeletion(@NotNull DBAIProfile profile) {
    Messages.showQuestionDialog(getProject(), txt(
                    "ai.settings.profile.deletion.title"), txt("ai.settings.profile.deletion.message.prefix", profile.getName()),
            Messages.OPTIONS_YES_NO, 1,
            option -> when(option == 0, () -> removeProfile(profile)));
  }

  public void markProfileAsDefault(@NotNull DBAIProfile profile) {
    manager.setDefaultProfile(getConnectionId(), profile);
  }

  @Nullable
  public DBAIProfile getSelectedProfile() {
    return profilesList.getSelectedValue();
  }

  public Set<String> getProfileNames() {
    DBObjectListModel<DBAIProfile> model = cast(profilesList.getModel());
    return model.getElements().stream().map(p -> p.getName()).collect(Collectors.toSet());
  }

  public void loadProfiles() {
    Background.run(getProject(), () -> doLoadProfiles(false));
  }

  public void reloadProfiles() {
    Background.run(getProject(), () -> doLoadProfiles(true));
  }

  private void doLoadProfiles(boolean force) {
    try {
      if (force) refreshUserObjects(getConnectionId(), AI_PROFILE);
      beforeProfilesLoad();
      List<DBAIProfile> profiles = manager.getProfiles(getConnectionId());
      applyProfiles(profiles);
    } catch (Exception e) {
      handleLoadError(e);
    } finally {
      afterProfilesLoad();
    }
  }

  private void handleLoadError(Throwable e) {
    conditionallyLog(e);
    Dispatch.run(mainPanel, () -> Messages.showErrorDialog(getProject(), "Failed to load profiles.\nCause: " + Exceptions.causeMessage(e)));
    afterProfilesLoad();
  }

  private void applyProfiles(List<DBAIProfile> profiles) {
    // capture selection
    DBAIProfile selectedProfile = profilesList.getSelectedValue();
    String selectedProfileName = selectedProfile == null ? null : selectedProfile.getName();

    // apply new profiles
    this.profileDetailForms = Disposer.replace(
            this.profileDetailForms,
            new ConcurrentHashMap<>());
    this.profilesList.setModel(Disposer.replace(
            profilesList.getModel(),
            DBObjectListModel.create(this, profiles)));



    // restore selection
    int selectionIndex = Lists.indexOf(profiles, c -> c.getName().equalsIgnoreCase(selectedProfileName));
    if (selectionIndex == -1 && !profiles.isEmpty()) selectionIndex = 0;
    if (selectionIndex != -1) this.profilesList.setSelectedIndex(selectionIndex);
  }

  private void beforeProfilesLoad() {
    loading = true;
    freezeForm();

    initializingIconPanel.setVisible(true);
    profilesList.setBackground(Colors.getPanelBackground());

  }

  private void afterProfilesLoad() {
    loading = false;
    unfreezeForm();

    initializingIconPanel.setVisible(false);
    profilesList.setBackground(Colors.getTextFieldBackground());
    profilesList.requestFocus();
    updateActionToolbars();
  }

  /**
   * Removes a profile from remote server
   *
   * @param profile the profile ot be deleted
   */
  private void removeProfile(DBAIProfile profile) {
    ObjectManagementService managementService = ObjectManagementService.getInstance(ensureProject());
    managementService.deleteObject(profile, null);
  }


  @NotNull
  public ConnectionHandler getConnection() {
    return connection.ensure();
  }

  private @NotNull ConnectionId getConnectionId() {
    return getConnection().getConnectionId();
  }

  @Nullable
  @Override
  public Object getData(@NotNull String dataId) {
    if (DataKeys.PROFILE_MANAGEMENT_FORM.is(dataId)) return this;
    return null;
  }
}
