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

package com.dbn.assistant.chat.window.action;

import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.common.util.Actions;
import com.dbn.object.DBAIProfile;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Action for selecting one individual AI-assistant profile
 *
 * @author Dan Cioca (Oracle)
 */
public class ProfileSelectAction extends AbstractChatBoxAction {
    private final DBObjectRef<DBAIProfile> profile;
    ProfileSelectAction(DBAIProfile profile) {
        this.profile = DBObjectRef.of(profile);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        ChatBoxForm chatBox = getChatBox(e);
        if (chatBox == null) return;

        chatBox.selectProfile(getProfile());
    }

    private DBAIProfile getProfile() {
        return DBObjectRef.ensure(profile);
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        DBAIProfile profile = getProfile();

        Presentation presentation = e.getPresentation();
        presentation.setText(Actions.adjustActionName(profile.getName()));
        presentation.setIcon(profile.getIcon());
        presentation.setEnabled(profile.isEnabled());
    }
}
