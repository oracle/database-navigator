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
import com.dbn.assistant.help.ui.AssistantHelpDialog;
import com.dbn.assistant.profile.wizard.ProfileEditionWizard;
import com.dbn.assistant.provider.AIModel;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.settings.ui.AssistantDatabaseConfigDialog;
import com.dbn.assistant.state.AssistantState;
import com.dbn.assistant.state.AssistantStateDelegate;
import com.dbn.common.action.Selectable;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.exception.Exceptions;
import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.message.MessageType;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.util.Popups;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SessionId;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.common.assistant.AssistantQueryResponse;
import com.dbn.database.interfaces.DatabaseAssistantInterface;
import com.dbn.object.DBAIProfile;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObjectBundle;
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

import javax.swing.JPanel;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.feature.FeatureAcknowledgement.ENGAGED;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.ui.CardLayouts.addCard;
import static com.dbn.common.ui.CardLayouts.isBlankCard;
import static com.dbn.common.ui.CardLayouts.showBlankCard;
import static com.dbn.common.ui.CardLayouts.showCard;
import static com.dbn.common.ui.CardLayouts.visibleCardId;
import static com.dbn.common.util.Conditional.when;
import static com.dbn.common.util.Lists.convert;
import static com.dbn.common.util.Lists.first;
import static com.dbn.common.util.Lists.firstElement;
import static com.dbn.common.util.Messages.options;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static java.util.Collections.emptyList;

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

  public List<DBAIProfile> getProfiles(ConnectionId connectionId) {
    if (connectionId == null) return emptyList();

    ConnectionHandler connection = ConnectionHandler.get(connectionId);
    if (connection == null) return emptyList();

    DBObjectBundle objectBundle = connection.getObjectBundle();
    DBSchema userSchema = objectBundle.getUserSchema();
    if (userSchema == null) return emptyList();

    return userSchema.getAIProfiles();
  }


  public void initializeAssistant(ConnectionId connectionId) {
    DBAIProfile defaultProfile = getDefaultProfile(connectionId);
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
      List<DBAIProfile> profiles = getProfiles(connectionId);
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

  public String query(ConnectionId connectionId, String prompt, ChatMessageContext context) throws SQLException {
    ConnectionHandler connection = ConnectionHandler.ensure(connectionId);

    String profile = context.getProfile();
    String action = context.getAction().getId();
    String attributes = context.getAttributes();

    DBNConnection conn = connection.getConnection(SessionId.ASSISTANT);
    DatabaseAssistantInterface assistantInterface = connection.getAssistantInterface();

    AssistantQueryResponse response = assistantInterface.generate(conn, action, profile, attributes, prompt);
    ProgressMonitor.checkCancelled();

    return response.read();
  }

  public void generate(ConnectionId connectionId, String text, ChatMessageContext context, Consumer<ChatMessage> consumer) {
    ConnectionHandler connection = ConnectionHandler.ensure(connectionId);
    Project project = getProject();

    Progress.modal(project, connection, true,
            getAssistantName(connectionId),
            getPromptText(context.getAction(), text), p -> {
      try {
        String content = query(connectionId, text, context);
        ChatMessage message = new ChatMessage(MessageType.NEUTRAL, content, AuthorType.AGENT, context);
        consumer.accept(message);
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
      DBAIProfile profile = getDefaultProfile(connectionId);
      if (profile != null) {
        AIProvider selectedProvider = profile.getProvider();
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
    DBAIProfile defaultProfile = getDefaultProfile(connectionId);
    List<ProfileSelectAction> actions = convert(getProfiles(connectionId), p -> new ProfileSelectAction(connectionId, p, defaultProfile));

    Popups.showActionsPopup("Select Profile", editor, actions, Selectable.selector());
  }

  public void setDefaultProfile(ConnectionId connectionId, @Nullable DBAIProfile profile) {
    getAssistantState(connectionId).setDefaultProfile(profile);
  }

  public void setSelectedProfile(ConnectionId connectionId, @Nullable DBAIProfile profile) {
    getAssistantState(connectionId).setSelectedProfile(profile);
  }

  public boolean isDefaultProfile(ConnectionId connectionId, DBAIProfile profile) {
    DBAIProfile defaultProfile = getDefaultProfile(connectionId);
    return Objects.equals(defaultProfile, profile);
  }

  @Nullable
  public DBAIProfile getDefaultProfile(ConnectionId connectionId) {
    List<DBAIProfile> profiles = getProfiles(connectionId);
    if (profiles.isEmpty()) return null;

    AssistantState assistantState = getAssistantState(connectionId);
    String profileName = assistantState.getDefaultProfileName();

    DBAIProfile profile = getProfile(connectionId, profileName);
    assistantState.setDefaultProfile(profile);
    return profile;
  }

  public DBAIProfile getSelectedProfile(ConnectionId connectionId) {
    List<DBAIProfile> profiles = getProfiles(connectionId);
    if (profiles.isEmpty()) return null;

    AssistantState assistantState = getAssistantState(connectionId);
    String profileName = assistantState.getSelectedProfileName();

    DBAIProfile profile = getProfile(connectionId, profileName);
    assistantState.setSelectedProfile(profile);
    return profile;
  }

  @Nullable
  private DBAIProfile getProfile(ConnectionId connectionId, String profileName) {
    List<DBAIProfile> profiles = getProfiles(connectionId);
    DBAIProfile profile = first(profiles, p -> p.getName().equalsIgnoreCase(profileName));

    if (profile == null) profile = firstElement(profiles);
    return profile;
  }

  @Nullable
  public AIModel getSelectedModel(ConnectionId connectionId) {
    DBAIProfile profile = getSelectedProfile(connectionId);
    if (profile == null) return null;

    AIProvider provider = profile.getProvider();
    if (provider == null) return null;

    AssistantState assistantState = getAssistantState(connectionId);
    String modelName = assistantState.getSelectedModelName();

    AIModel model = provider.getModel(modelName);
    return model == null ? provider.getDefaultModel() : model;
  }

  public boolean isPromptingAvailable(ConnectionId connectionId) {
    AssistantState assistantState = getAssistantState(connectionId);
    if (!assistantState.isAvailable()) return false;

    DBAIProfile profile = getSelectedProfile(connectionId);
    if (profile == null) return false;
    if (!profile.isEnabled()) return false;

    return true;
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
}
