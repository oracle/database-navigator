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

package com.dbn.assistant.state;

import com.dbn.assistant.DatabaseAssistantType;
import com.dbn.assistant.chat.message.PersistentChatMessage;
import com.dbn.assistant.chat.window.PromptAction;
import com.dbn.assistant.entity.AIProfileItem;
import com.dbn.assistant.entity.Profile;
import com.dbn.common.feature.FeatureAcknowledgement;
import com.dbn.common.feature.FeatureAvailability;
import com.dbn.common.property.PropertyHolderBase;
import com.dbn.common.state.PersistentStateElement;
import com.dbn.connection.ConnectionId;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dbn.common.options.setting.Settings.*;
import static com.dbn.common.util.Commons.coalesce;
import static com.dbn.common.util.Lists.*;

/**
 * Assistant state holder
 * This class represents the state of the DB Assistant for a given connection, as well as the chat-box state.
 * It encapsulates the current profiles, selected profile,
 * a history of questions, the AI answers, and the current connection.
 *
 *
 * @author Ayoub Aarrasse (Oracle)
 * @author Emmanuel Jannetti (Oracle)
 */
@Setter
@Getter
@NoArgsConstructor
public class AssistantState extends PropertyHolderBase.IntStore<AssistantStatus> implements PersistentStateElement {

  private FeatureAvailability availability = FeatureAvailability.UNCERTAIN;
  private FeatureAcknowledgement acknowledgement = FeatureAcknowledgement.NONE;

  private ConnectionId connectionId;
  private DatabaseAssistantType assistantType = DatabaseAssistantType.GENERIC;
  private List<AIProfileItem> profiles = new ArrayList<>();
  private List<PersistentChatMessage> messages = new ArrayList<>();

  private PromptAction selectedAction = PromptAction.SHOW_SQL;
  private String defaultProfileName;

  public static final short MAX_CHAR_MESSAGE_COUNT = 100;

  public AssistantState(ConnectionId connectionId) {
    this.connectionId = connectionId;
  }

  @Override
  protected AssistantStatus[] properties() {
    return AssistantStatus.VALUES;
  }

  public String getAssistantName() {
    switch (assistantType) {
      case SELECT_AI: return txt("app.assistant.title.DatabaseAssistantName_SELECT_AI");
      case GENERIC:
      default: return txt("app.assistant.title.DatabaseAssistantName_GENERIC");
    }
  }

  public boolean isSupported() {
    return availability == FeatureAvailability.AVAILABLE;
  }

  /**
   * State utility indicating the feature is initialized and ready to use
   * @return true if the chat box is properly initialized and can be interacted with
   */
  public boolean isAvailable() {
    return isSupported() &&
            isNot(AssistantStatus.INITIALIZING) &&
            isNot(AssistantStatus.UNAVAILABLE) &&
            isNot(AssistantStatus.QUERYING);
  }

  /**
   * State utility indicating the prompting is available.
   * It internally checks if the feature is ready to use by calling {@link #isAvailable()} but also checks if a valid profile is selected
   * @return true if prompting is allowed
   */
  public boolean isPromptingAvailable() {
    if (!isAvailable()) return false;

    AIProfileItem profile = getSelectedProfile();
    if (profile == null) return false;
    if (!profile.isEnabled()) return false;

    return true;
  }

  @Nullable
  public String getSelectedProfileName() {
    AIProfileItem selectedProfile = getSelectedProfile();
    return selectedProfile == null ? null : selectedProfile.getName();
  }

  @Nullable
  public AIProfileItem getSelectedProfile() {
    return first(profiles, p -> p.isSelected());
  }

  public void setSelectedProfile(@Nullable AIProfileItem profile) {
    String profileName = profile == null ? null : profile.getName();
    forEach(profiles, p -> p.setSelected(Objects.equals(p.getName(), profileName)));
  }

  public void importProfiles(List<Profile> profiles) {
    String selectedProfile = getSelectedProfileName();
    setProfiles(convert(profiles, p -> new AIProfileItem(p, p.getProfileName().equalsIgnoreCase(selectedProfile))));
  }

  /**
   *
   * Replaces the list of profiles by preserving the profile and model selection (as far as possible)
   * @param profiles
   */
  public void setProfiles(List<AIProfileItem> profiles) {
    this.profiles = profiles;

    AIProfileItem selectedProfile = getSelectedProfile();
    if (selectedProfile == null) setSelectedProfile(firstElement(profiles));
  }

  public Set<String> getProfileNames() {
    return profiles.stream().map(p -> p.getName()).collect(Collectors.toSet());
  }

  @Nullable
  public AIProfileItem getDefaultProfile() {
    // resolve default profile by doing ever less qualified lookup inside the list of profiles
    return coalesce(
            () -> first(profiles, p -> p.isEnabled() && p.getName().equalsIgnoreCase(defaultProfileName)),
            () -> first(profiles, p -> p.isEnabled() && p.isSelected()),
            () -> first(profiles, p -> p.isEnabled()));
  }

  public void setDefaultProfile(@Nullable AIProfileItem profile) {
    defaultProfileName = profile == null ? null : profile.getName();
  }

  public void addMessages(List<PersistentChatMessage> messages) {
    this.messages.addAll(messages);
  }


  public void clearMessages() {
    messages.clear();
  }

  @Override
  public void readState(Element element) {
    connectionId = connectionIdAttribute(element, "connection-id");
    defaultProfileName = stringAttribute(element, "default-profile-name");
    assistantType = enumAttribute(element, "assistant-type", assistantType);
    selectedAction = enumAttribute(element, "selected-action", selectedAction);
    availability = enumAttribute(element, "availability", availability);
    acknowledgement = enumAttribute(element, "acknowledgement", acknowledgement);

    List<AIProfileItem> profiles = new ArrayList<>();
    Element profilesElement = element.getChild("profiles");
    List<Element> profileElements = profilesElement.getChildren();
    for (Element profileElement : profileElements) {
      AIProfileItem profile = new AIProfileItem();
      profile.readState(profileElement);
      profiles.add(profile);
    }
    setProfiles(profiles);

    Element messagesElement = element.getChild("messages");
    List<Element> messageElements = messagesElement.getChildren();
    for (Element messageElement : messageElements) {
      PersistentChatMessage message = new PersistentChatMessage();
      message.readState(messageElement);
      messages.add(message);
    }
  }

  @Override
  public void writeState(Element element) {
    setStringAttribute(element, "connection-id", connectionId.id());
    setStringAttribute(element, "default-profile-name", defaultProfileName);
    setEnumAttribute(element, "assistant-type", assistantType);
    setEnumAttribute(element, "selected-action", selectedAction);
    setEnumAttribute(element, "availability", availability);
    setEnumAttribute(element, "acknowledgement", acknowledgement);

    Element profilesElement = newElement(element, "profiles");
    for (AIProfileItem profile : profiles) {
      Element profileElement = newElement(profilesElement, "profile");
      profile.writeState(profileElement);
    }

    Element messagesElement = newElement(element, "messages");
    for (PersistentChatMessage message : messages) {
      Element messageElement = newElement(messagesElement, "message");
      message.writeState(messageElement);
    }
  }
}
