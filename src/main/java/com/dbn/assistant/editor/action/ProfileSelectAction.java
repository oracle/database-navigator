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

package com.dbn.assistant.editor.action;

import com.dbn.assistant.DatabaseAssistantManager;
import com.dbn.common.action.ProjectAction;
import com.dbn.common.action.Selectable;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionId;
import com.dbn.object.DBAIProfile;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class ProfileSelectAction extends ProjectAction implements Selectable {
    private final ConnectionId connectionId;
    private final DBObjectRef<DBAIProfile> profile;
    private final boolean selected;

    public ProfileSelectAction(ConnectionId connectionId, @NotNull DBAIProfile profile, @Nullable DBAIProfile defaultProfile) {
        this.connectionId = connectionId;
        this.profile = DBObjectRef.of(profile);
        this.selected = defaultProfile != null && Strings.equalsIgnoreCase(profile.getName(), defaultProfile.getName());
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        DatabaseAssistantManager manager = DatabaseAssistantManager.getInstance(project);
        manager.setDefaultProfile(connectionId, DBObjectRef.get(profile));
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        String name = Actions.adjustActionName(getProfileName());
        e.getPresentation().setText(name);
    }

    public String getProfileName() {
        return profile.getObjectName();
    }
}
