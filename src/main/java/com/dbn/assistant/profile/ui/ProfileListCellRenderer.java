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
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.nls.NlsSupport;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static com.intellij.ui.SimpleTextAttributes.GRAY_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES;

public class ProfileListCellRenderer extends ColoredListCellRenderer<Profile> implements NlsSupport {
    private final ConnectionRef connection;

    public ProfileListCellRenderer(ConnectionHandler connection) {
        this.connection = ConnectionRef.of(connection);
    }

    private ConnectionHandler getConnection() {
        return ConnectionRef.ensure(connection);
    }

    @Override
    protected void customizeCellRenderer(@NotNull JList<? extends Profile> list, Profile profile, int index, boolean selected, boolean hasFocus) {
        String profileName = profile.getProfileName();
        boolean enabled = profile.isEnabled();
        SimpleTextAttributes attributes = enabled ? REGULAR_ATTRIBUTES : GRAY_ATTRIBUTES;
        append(profileName, attributes);
        if (isDefault(profile)) append(" (default)", attributes);

        setToolTipText(enabled ? null : txt("ai.settings.profile.not_enabled"));
    }

    private boolean isDefault(Profile profile) {
        Project project = getConnection().getProject();
        DatabaseAssistantManager assistantManager = DatabaseAssistantManager.getInstance(project);
        AIProfileItem defaultProfile = assistantManager.getDefaultProfile(connection.getConnectionId());
        return defaultProfile != null && defaultProfile.getName().equalsIgnoreCase(profile.getProfileName());
    }
}
