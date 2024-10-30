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

package com.dbn.assistant.profile.action;

import com.dbn.assistant.DatabaseAssistantManager;
import com.dbn.assistant.entity.AIProfileItem;
import com.dbn.assistant.entity.Profile;
import com.dbn.assistant.profile.ui.ProfileManagementForm;
import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectionId;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Profile management action - mark profile as default
 * (default profiles are used in the editor AI interactions)
 *
 * @author Dan Cioca (Oracle)
 */
public class ProfileDefaultAction extends ProfileManagementAction {
    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        ProfileManagementForm managementForm = getManagementForm(e);
        if (managementForm == null) return;

        Profile profile = managementForm.getSelectedProfile();
        if (profile == null) return;

        managementForm.markProfileAsDefault(profile);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setIcon(Icons.ACTION_CHECK);
        presentation.setText("Mark as Default");
        presentation.setEnabled(isEnabled(e));
    }

    private static boolean isEnabled(@NotNull AnActionEvent e) {
        ProfileManagementForm managementForm = getManagementForm(e);
        if (managementForm == null) return false;
        if (managementForm.isLoading()) return false;

        Profile profile = managementForm.getSelectedProfile();
        if (profile == null) return false;
        if (!profile.isEnabled()) return false;

        ConnectionId connectionId = managementForm.getConnection().getConnectionId();
        return !isDefault(e, connectionId, profile);
    }

    private static boolean isDefault(@NotNull AnActionEvent e, ConnectionId connectionId, Profile profile) {
        Project project = getEventProject(e);
        if (project == null) return false;

        DatabaseAssistantManager assistantManager = DatabaseAssistantManager.getInstance(project);
        AIProfileItem defaultProfile = assistantManager.getDefaultProfile(connectionId);
        return defaultProfile != null && defaultProfile.getName().equalsIgnoreCase(profile.getProfileName());
    }
}
