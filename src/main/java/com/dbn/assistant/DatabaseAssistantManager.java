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

package com.dbn.assistant;

import com.dbn.DatabaseNavigator;
import com.dbn.assistant.chat.message.AuthorType;
import com.dbn.assistant.chat.message.ChatMessage;
import com.dbn.assistant.chat.message.ChatMessageContext;
import com.dbn.assistant.chat.window.PromptAction;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.assistant.editor.action.ProfileSelectAction;
import com.dbn.assistant.entity.AIProfileItem;
import com.dbn.assistant.entity.Profile;
import com.dbn.assistant.help.ui.AssistantHelpDialog;
import com.dbn.assistant.profile.wizard.ProfileEditionWizard;
import com.dbn.assistant.provider.ProviderType;
import com.dbn.assistant.service.*;
import com.dbn.assistant.service.mock.FakeAICredentialService;
import com.dbn.assistant.service.mock.FakeAIProfileService;
import com.dbn.assistant.service.mock.FakeDatabaseService;
import com.dbn.assistant.settings.ui.AssistantDatabaseConfigDialog;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.state.AssistantStateDelegate;
import com.dbn.common.action.Selectable;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.exception.Exceptions;
import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.message.MessageType;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.util.Popups;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionStatusListener;
import com.dbn.connection.SessionId;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.common.assistant.AssistantQueryResponse;
import com.dbn.database.interfaces.DatabaseAssistantInterface;
import com.dbn.object.event.ObjectChangeListener;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.util.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.assistant.state.AssistantStatus.UNAVAILABLE;
import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.feature.FeatureAcknowledgement.ENGAGED;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.ui.CardLayouts.*;
import static com.dbn.common.util.Conditional.when;
import static com.dbn.common.util.Lists.convert;
import static com.dbn.common.util.Messages.options;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

/**
 * Main database AI-Assistance management component
 *
 * @author Ayoub Aarrasse (Oracle)
 * @author Emmanuel Jannetti (Oracle)
 */
@Slf4j
@State(
    name = DatabaseAssistantManager.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE))
public class DatabaseAssistantManager extends ProjectComponentBase implements PersistentState {
  public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseAssistantManager";
  public static final String TOOL_WINDOW_ID = "DB Assistant";

  private final Map<ConnectionId, AssistantState> assistantStates = new ConcurrentHashMap<>();
  private final Map<ConnectionId, ChatBoxForm> chatBoxes = new ConcurrentHashMap<>();

  private DatabaseAssistantManager(Project project) {
    super(project, COMPONENT_NAME);

    initChangeListener();
    initConnectivityListener();
  }

  public static DatabaseAssistantManager getInstance(@NotNull Project project) {
    return projectService(project, DatabaseAssistantManager.class);
  }

  public void showToolWindow(@Nullable ConnectionId connectionId) {
    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(getProject());
    ToolWindow toolWindow = Failsafe.nn(toolWindowManager.getToolWindow(TOOL_WINDOW_ID));
    toolWindow.show(null);
    switchToConnection(connectionId);
  }

  public String getAssistantName(@Nullable ConnectionId connectionId) {
    String defaultName = txt("app.assistant.title.DatabaseAssistantName_GENERIC");
    if (connectionId == null) return defaultName;

    AssistantState assistantState = getAssistantState(connectionId);
    return assistantState.getAssistantName();
  }

  public List<Profile> getAssistantProfiles(@Nullable ConnectionId connectionId) {
    if (connectionId == null) return Collections.emptyList();

    AIProfileService profileService = getProfileService(connectionId);
    return profileService.list().join();
  }


  public void initializeAssistant(ConnectionId connectionId) {
    AIProfileItem defaultProfile = getDefaultProfile(connectionId);
    if (defaultProfile != null) return;

    AssistantState assistantState = getAssistantState(connectionId);
    if (assistantState.getAcknowledgement() == ENGAGED) {
      // assistant not yet configured -> prompt modal initialization
      promptMissingProfiles(connectionId);
    } else {
      // assistant not yet acknowledged -> show acknowledgment popup
      promptAcknowledgement(connectionId);
    }
  }

  public void promptMissingProfiles(ConnectionId connectionId) {
    ConnectionHandler connection = ConnectionHandler.ensure(connectionId);
    Project project = getProject();
    Progress.modal(project, connection, true, "Initializing DB Assistant", "Initializing database assistant", progress -> {
      List<Profile> profiles = getAssistantProfiles(connectionId);
      // no profiles created yet -> prompt profile creation
      if (profiles.isEmpty()) {
        Messages.showQuestionDialog(project,
                getAssistantName(connectionId),
                txt("msg.assistant.question.AcknowledgeAndCreateProfile"),
                options("Create Profile", "Cancel"), 0,
                option -> when(option == 0, () -> ProfileEditionWizard.showWizard(connection, null, Collections.emptySet(), null)));
      }
    });
  }

  private void promptAcknowledgement(ConnectionId connectionId) {
    Project project = getProject();
    Messages.showQuestionDialog(project,
            getAssistantName(connectionId),
            txt("msg.assistant.question.AcknowledgeAndConfigure"),
            Messages.OPTIONS_CONTINUE_CANCEL, 0,
            option -> when(option == 0, () -> showToolWindow(connectionId)));
  }

  /**
   * switch from current connection to the new selected one from DBN navigator
   *
   * @param connectionId the new selected connection
   */
  public void switchToConnection(@Nullable ConnectionId connectionId) {
    JPanel toolWindowPanel = getToolWindowPanel();
    String id = visibleCardId(toolWindowPanel);
    ConnectionId selectedConnectionId = isBlankCard(id) ? null : ConnectionId.get(id);

    if (Objects.equals(selectedConnectionId, connectionId)) return;
    initToolWindow(connectionId);
  }

  @Nullable
  private ChatBoxForm getChatBox(@Nullable ConnectionId connectionId) {
    if (connectionId == null) return null;

    ConnectionHandler connection = ConnectionHandler.get(connectionId);
    if (connection == null) return null;
    // TODO clarify - present assistant for unsupported databases?
    //if (!AI_ASSISTANT.isSupported(connection)) return null;

    return chatBoxes.computeIfAbsent(connectionId, id -> {
      ChatBoxForm chatBox = new ChatBoxForm(connection);
      addCard(getToolWindowPanel(), chatBox, connectionId);
      return chatBox;
    });
  }

  public AssistantState getAssistantState(ConnectionId connectionId) {
    return assistantStates.computeIfAbsent(connectionId, c -> new AssistantStateDelegate(getProject(), c));
  }

  public ToolWindow getToolWindow() {
    ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(getProject());
    return toolWindowManager.getToolWindow(TOOL_WINDOW_ID);
  }

  private JPanel getToolWindowPanel() {
    Content content = getToolWindow().getContentManager().getContent(0);
    return (JPanel) content.getComponent();
  }

  private void initToolWindow(ConnectionId connectionId) {
    ToolWindow toolWindow = getToolWindow();
    JPanel toolWindowPanel = getToolWindowPanel();

    ChatBoxForm chatBox = getChatBox(connectionId);
    if (chatBox == null) {
      showBlankCard(toolWindowPanel);
    } else {
      showCard(toolWindowPanel, connectionId);
      toolWindow.setAvailable(true);
    }
  }

  public CompletableFuture<String> queryAssistant(ConnectionId connectionId, String text, String action,
                                                  String profile, String model) {
    return CompletableFuture.supplyAsync(() -> {
//      return "```\nselect something;\n```\n\nTHIS IS a response " + new Random().nextInt();
      try {
        ConnectionHandler connection = ConnectionHandler.ensure(connectionId);
        DBNConnection conn = connection.getConnection(SessionId.ASSISTANT);
        return connection.getAssistantInterface()
                .executeQuery(conn, action, profile, text, model)
                .getQueryOutput();
      } catch (SQLException e) {
        throw new CompletionException(e);
      }
    });
  }

  public void generate(ConnectionId connectionId, String text, ChatMessageContext context, Consumer<ChatMessage> consumer) {
    ConnectionHandler connection = ConnectionHandler.ensure(connectionId);
    Project project = getProject();

    Progress.modal(project, connection, true,
            getAssistantName(connectionId),
            getPromptText(context.getAction(), text), p -> {
      try {

        String profile = context.getProfile();
        String action = context.getAction().getId();
        String model = context.getModel().getApiName();

        DBNConnection conn = connection.getConnection(SessionId.ASSISTANT);
        DatabaseAssistantInterface assistantInterface = connection.getAssistantInterface();
        AssistantQueryResponse response = assistantInterface.generate(conn, action, profile, model, text);
        ProgressMonitor.checkCancelled();

        String content = response.read();
        consumer.accept(new ChatMessage(MessageType.NEUTRAL, content, AuthorType.AGENT, context));

      } catch (ProcessCanceledException e) {
        conditionallyLog(e);
        throw e;
      } catch (Exception e) {
        conditionallyLog(e);
        handleGenerateException(project, connectionId, e);
      }
    });
  }

  private void handleGenerateException(Project project, ConnectionId connectionId, Exception e) {
    String assistantName = getAssistantName(connectionId);
    String title = assistantName + " Error";

    String message = getPresentableMessage(connectionId, e);

    Messages.showErrorDialog(project, title,
            message, options("Help", "Cancel"), 0,
            option -> when(option == 0, () -> showPrerequisitesDialog(connectionId)));
  }

  public String getPresentableMessage(ConnectionId connectionId, Throwable e) {
    // todo move logic to OracleMessageParserInterface

    e = Exceptions.rootCauseOf(e);
    String assistantName = getAssistantName(connectionId);
    String errorMessage = e.getMessage();
    boolean networkAccessDenied = errorMessage != null && errorMessage.contains("ORA-24247");

    if (networkAccessDenied) {
      AIProfileItem profile = getDefaultProfile(connectionId);
      if (profile != null) {
        ProviderType selectedProvider = profile.getProvider();
        String accessPoint = selectedProvider.getHost();

        return txt("msg.assistant.error.NetworkAccessDenied", accessPoint, errorMessage);
      }
    }

    return txt("msg.assistant.error.AssistantInvocationFailure", assistantName, errorMessage);
  }

  public void showPrerequisitesDialog(ConnectionId connectionId) {
    ConnectionHandler connection = ConnectionHandler.ensure(connectionId);
    Dialogs.show(() -> new AssistantHelpDialog(connection));
  }

  private String getPromptText(PromptAction action, String prompt) {
    switch (action) {
      case SHOW_SQL: return txt("prc.assistant.message.PromptAction_SHOW_SQL", prompt);
      case EXPLAIN_SQL: return txt("prc.assistant.message.PromptAction_EXPLAIN_SQL", prompt);
      case EXECUTE_SQL: return txt("prc.assistant.message.PromptAction_EXECUTE_SQL", prompt);
      case NARRATE: return txt("prc.assistant.message.PromptAction_NARRATE", prompt);
      case CHAT: return txt("prc.assistant.message.PromptAction_CHAT", prompt);
      default: return txt("prc.assistant.message.PromptAction_ANY", prompt);
    }
  }

  public void openProfileConfiguration(ConnectionId connectionId) {
    ConnectionHandler connection = ConnectionHandler.ensure(connectionId);
    Dialogs.show(() -> new AssistantDatabaseConfigDialog(connection));
  }

  public void promptProfileSelector(Editor editor, ConnectionId connectionId) {
    AssistantState assistantState = getAssistantState(connectionId);
    AIProfileItem defaultProfile = getDefaultProfile(connectionId);
    List<ProfileSelectAction> actions = convert(assistantState.getProfiles(), p -> new ProfileSelectAction(connectionId, p, defaultProfile));

    Popups.showActionsPopup("Select Profile", editor, actions, Selectable.selector());
  }

  /*********************************************
   *            PersistentStateComponent       *
   *********************************************/

  @Override
  public Element getComponentState() {
    Element element = newElement("state");
    Element statesElement = newElement(element, "assistants");
    for (ConnectionId connectionId : assistantStates.keySet()) {
      AssistantState state = assistantStates.get(connectionId);
      Element stateElement = newElement(statesElement, "assistant-state");
      state.writeState(stateElement);
    }
    return element;
  }

  @Override
  public void loadComponentState(@NotNull Element element) {
    Element statesElement = element.getChild("assistants");
    if (statesElement != null) {
      List<Element> stateElements = statesElement.getChildren();
      for (Element stateElement : stateElements) {
        AssistantState state = new AssistantStateDelegate(getProject(), null);
        state.readState(stateElement);
        assistantStates.put(state.getConnectionId(), state);
      }
    }
  }

  private final Map<ConnectionId, AIProfileService> profileManagerMap = new ConcurrentHashMap<>();

  private final Map<ConnectionId, AICredentialService> credentialManagerMap = new ConcurrentHashMap<>();

  private final Map<ConnectionId, DatabaseService> databaseManagerMap = new ConcurrentHashMap<>();


  /**
   * Gets a profile manager for the current connection.
   * Managers are singletons
   * Ww assume that we always have a current connection
   *
   * @return a manager.
   */
  public AIProfileService getProfileService(ConnectionId connectionId) {
    return profileManagerMap.computeIfAbsent(
            connectionId,
            id -> new AIProfileService.CachedProxy((isMockEnv() ?
                    new FakeAIProfileService() :
                    new AIProfileServiceImpl(ConnectionHandler.ensure(id)))));
  }

  public AICredentialService getCredentialService(ConnectionId connectionId) {
    return credentialManagerMap.computeIfAbsent(connectionId,
            id -> new AICredentialService.CachedProxy(isMockEnv() ?
                    new FakeAICredentialService() :
                    new AICredentialServiceImpl(ConnectionHandler.ensure(id))));
  }

  public DatabaseService getDatabaseService(ConnectionId connectionId) {
    return databaseManagerMap.computeIfAbsent(connectionId, id ->
            isMockEnv() ?
                    new FakeDatabaseService() :
                    new DatabaseServiceImpl(ConnectionHandler.ensure(id)));
  }

  private static boolean isMockEnv() {
    return Boolean.parseBoolean(System.getProperty("fake.services"));
  }

  public AIProfileItem getDefaultProfile(ConnectionId connectionId) {
    return getAssistantState(connectionId).getDefaultProfile();
  }

  public void setDefaultProfile(ConnectionId connectionId, AIProfileItem profile) {
    getAssistantState(connectionId).setDefaultProfile(profile);
  }

  private void initChangeListener() {
    ProjectEvents.subscribe(ensureProject(), this, ObjectChangeListener.TOPIC, (connectionId, ownerId, objectType) -> {
      if (objectType != DBObjectType.PROFILE) return;

      // it is assumed that DB assistant is supported if a change listener on PROFILE object type was fired
      AssistantState assistantState = assistantStates.get(connectionId);
      refreshStateProfiles(connectionId, assistantState);
    });
  }

  /**
   * Attempts to load the profiles when connectivity is restored if the connection was down on first initialization attempt
   */
  private void initConnectivityListener() {
    ProjectEvents.subscribe(ensureProject(), this, ConnectionStatusListener.TOPIC, (connectionId, sessionId) -> {
      AssistantState assistantState = getAssistantState(connectionId);
      if (assistantState.isNot(UNAVAILABLE)) return;
      refreshStateProfiles(connectionId, assistantState);
    });
  }

  private void refreshStateProfiles(ConnectionId connectionId, AssistantState assistantState) {
    Background.run(getProject(), () -> assistantState.importProfiles(getAssistantProfiles(connectionId)));
  }

}
