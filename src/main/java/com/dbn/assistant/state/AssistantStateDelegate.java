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
import com.dbn.assistant.chat.window.PromptAction;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.feature.FeatureAcknowledgement;
import com.dbn.common.feature.FeatureAvailability;
import com.dbn.common.project.ProjectRef;
import com.dbn.connection.ConnectionId;
import com.intellij.openapi.project.Project;

import java.util.Objects;

/**
 * Monitored delegation of {@link AssistantState} allowing to notify changes of the state
 * to listeners of type {@link AssistantStateListener}
 *
 * @author Dan Cioca (Oracle)
 */
public class AssistantStateDelegate extends AssistantState {
    private final ProjectRef project;

    public AssistantStateDelegate(Project project, ConnectionId connectionId) {
        super(connectionId);
        this.project = ProjectRef.of(project);
    }

    public void setAvailability(FeatureAvailability availability) {
        if (getAvailability() == availability) return;

        super.setAvailability(availability);
        notifyStateListeners();
    }

    public void setAssistantType(DatabaseAssistantType assistantType) {
        if (getAssistantType() == assistantType) return;

        super.setAssistantType(assistantType);
        notifyStateListeners();
    }

    public void setAcknowledgement(FeatureAcknowledgement acknowledgement) {
        if (getAcknowledgement() == acknowledgement) return;

        super.setAcknowledgement(acknowledgement);
        notifyStateListeners();
    }

    @Override
    public void setSelectedAction(PromptAction selectedAction) {
        if (getSelectedAction() == selectedAction) return;
        super.setSelectedAction(selectedAction);
        notifyStateListeners();
    }

    @Override
    public void setDefaultProfileName(String profileName) {
        if (Objects.equals(getDefaultProfileName(), profileName)) return;

        super.setDefaultProfileName(profileName);
        notifyStateListeners();
    }

    @Override
    public void setSelectedProfileName(String profileName) {
        if (Objects.equals(getSelectedProfileName(), profileName)) return;

        super.setSelectedProfileName(profileName);
        notifyStateListeners();
    }

    @Override
    public void setSelectedModelName(String modelName) {
        if (Objects.equals(getSelectedModelName(), modelName)) return;

        super.setSelectedModelName(modelName);
        notifyStateListeners();
    }

    @Override
    protected void changed(AssistantStatus property, boolean value) {
        notifyStateListeners();
    }

    private void notifyStateListeners() {
        Project project = getProject();
        ProjectEvents.notify(project, AssistantStateListener.TOPIC, l -> l.stateChanged(project, getConnectionId()));
    }

    private Project getProject() {
        return ProjectRef.ensure(project);
    }

}
