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
import com.dbn.assistant.entity.AIProfileItem;
import com.dbn.assistant.entity.Profile;
import com.dbn.assistant.profile.wizard.ProfileEditionWizard;
import com.dbn.assistant.service.AIProfileService;
import com.dbn.common.action.DataKeys;
import com.dbn.common.color.Colors;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.exception.Exceptions;
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
import com.dbn.object.event.ObjectChangeListener;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.util.ui.AsyncProcessIcon;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.dbn.common.util.Conditional.when;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

/**
 * Profile management bindings
 *
 * @author Ayoub Aarrasse (Oracle)
 * @author Emmanuel Jannetti (Oracle)
 */
public class ProfileManagementForm extends DBNFormBase {

  private JPanel mainPanel;
  private JButton goToAssociatedObjects;
  private JList<Profile> profilesList;
  private JPanel detailPanel;
  private JPanel actionsPanel;
  private JPanel initializingIconPanel;
  private JSplitPane contentSplitPane;

  private final ConnectionRef connection;
  private final DatabaseAssistantManager manager;
  private final AIProfileService profileSvc;

  private List<Profile> profiles = new ArrayList<>();
  private Map<String, ProfileDetailsForm> profileDetailForms = new ConcurrentHashMap<>();

  @Getter
  private boolean loading;


  public ProfileManagementForm(DBNForm parent, @NotNull ConnectionHandler connection) {
    super(parent);
    this.connection = ConnectionRef.of(connection);

    this.manager = DatabaseAssistantManager.getInstance(connection.getProject());
    this.profileSvc = AIProfileService.getInstance(connection);

    initActionsPanel();
    initProfilesList();
    initDetailsPanel();
    initChangeListener();

    whenShown(() -> loadProfiles());
  }

  private void initChangeListener() {
    ProjectEvents.subscribe(ensureProject(), this, ObjectChangeListener.TOPIC, (connectionId, ownerId, objectType) -> {
      if (connectionId != getConnection().getConnectionId()) return;
      if (objectType != DBObjectType.PROFILE) return;
      reloadProfiles();
    });
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  /**
   * initialize bindings
   */
  private void initComponent() {


/*    goToAssociatedObjects.addActionListener(event -> {
      ProfileEditionWizard.showWizard(getConnection(), getSelectedProfile(), getProfileNames(), isCommit -> {
        if (isCommit) loadProfiles();
      }, ProfileEditionObjectListStep.class);
    });*/

  }

  private void initProfilesList() {
    profilesList.setBackground(Colors.getTextFieldBackground());
    profilesList.setBorder(Borders.EMPTY_BORDER);

    profilesList.addListSelectionListener((e) -> {
      if (e.getValueIsAdjusting()) return;

      Profile selectedProfile = profilesList.getSelectedValue();
      showDetailForm(selectedProfile);
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

  public void showDetailForm(Profile profile) {
    if (profile == null) {
      CardLayouts.showBlankCard(detailPanel);
    } else {
      String profileName = profile.getProfileName();
      profileDetailForms.computeIfAbsent(profileName, c -> createDetailForm(profile));
      CardLayouts.showCard(detailPanel, profileName);
    }
  }

  @NotNull
  private ProfileDetailsForm createDetailForm(Profile profile) {
    ProfileDetailsForm detailsForm = new ProfileDetailsForm(this, profile);
    CardLayouts.addCard(detailPanel, detailsForm.getComponent(), profile.getProfileName());
    return detailsForm;
  }

  public void promptProfileCreation() {
    ProfileEditionWizard.showWizard(getConnection(), null, getProfileNames(), null);
  }

  public void promptProfileEdition(@NotNull Profile profile) {
    ProfileEditionWizard.showWizard(getConnection(), profile, getProfileNames(), null);
  }

  public void promptProfileDeletion(@NotNull Profile profile) {
    Messages.showQuestionDialog(getProject(), txt(
                    "ai.settings.profile.deletion.title"), txt("ai.settings.profile.deletion.message.prefix", profile.getProfileName()),
            Messages.OPTIONS_YES_NO, 1,
            option -> when(option == 0, () -> removeProfile(profile)));
  }

  public void markProfileAsDefault(@NotNull Profile profile) {
    manager.setDefaultProfile(getConnectionId(), new AIProfileItem(profile, false));
  }

  @Nullable
  public Profile getSelectedProfile() {
    return profilesList.getSelectedValue();
  }

  @Nullable
  public Profile getDefaultProfile() {
    return profilesList.getSelectedValue();
  }

  public Set<String> getProfileNames() {
    return profiles.stream().map(p -> p.getProfileName()).collect(Collectors.toSet());
  }

  public void reloadProfiles() {
    profileSvc.reset();
    loadProfiles();
  }

  public void loadProfiles() {
    beforeLoad();
    profileSvc.list().thenAccept(profiles -> {
      try {
        applyProfiles(profiles);
        afterLoad();
      } catch (Exception e) {
        handleLoadError(e);
      }
    }).exceptionally(e -> {
      handleLoadError(e);
      return null;
    });
  }

  private void handleLoadError(Throwable e) {
    conditionallyLog(e);
    Dispatch.run(mainPanel, () -> Messages.showErrorDialog(getProject(), "Failed to load profiles.\nCause: " + Exceptions.causeMessage(e)));
    afterLoad();
  }

  private void applyProfiles(List<Profile> profiles) {
    // capture selection
    Profile selectedProfile = getSelectedProfile();
    String selectedProfileName = selectedProfile == null ? null : selectedProfile.getProfileName();

    // apply new profiles
    this.profiles = profiles;
    this.profileDetailForms = Disposer.replace(this.profileDetailForms, new ConcurrentHashMap<>());
    this.profilesList.setListData(profiles.toArray(new Profile[0]));

    // restore selection
    int selectionIndex = Lists.indexOf(profiles, c -> c.getProfileName().equalsIgnoreCase(selectedProfileName));
    if (selectionIndex == -1 && !profiles.isEmpty()) selectionIndex = 0;
    if (selectionIndex != -1) this.profilesList.setSelectedIndex(selectionIndex);
  }

  private void beforeLoad() {
    loading = true;
    initializingIconPanel.setVisible(true);
    profilesList.setBackground(Colors.getPanelBackground());
  }

  private void afterLoad() {
    loading = false;
    initializingIconPanel.setVisible(false);
    profilesList.setBackground(Colors.getTextFieldBackground());
    profilesList.requestFocus();
  }

  /**
   * Removes a profile from remote server
   *
   * @param profile the profile ot be deleted
   */
  private void removeProfile(Profile profile) {
    profileSvc.delete(profile.getProfileName()).thenRun(() -> loadProfiles()).exceptionally(throwable -> {
      Messages.showErrorDialog(getProject(),
          txt("profiles.mgnt.attr.deletion.failed.title"),
          txt("profiles.mgnt.attr.deletion.failed.msg"));

      return null;
    });
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
