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
import com.dbn.assistant.provider.AIModel;
import com.dbn.common.action.ComboBoxAction;
import com.dbn.common.action.DataKeys;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Lists;
import com.dbn.object.DBAIProfile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.util.Collections;
import java.util.List;

/**
 * Action for selecting the current AI-assistant model
 *
 * @author Dan Cioca (Oracle)
 */
public class ModelSelectDropdownAction extends ComboBoxAction implements DumbAware {
    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(JComponent component, DataContext dataContext) {
        List<AIModel> models = getProviderModels(dataContext);

        DefaultActionGroup actionGroup = new DefaultActionGroup();
        Lists.forEach(models, m -> actionGroup.add(new ModelSelectAction(m)));

        return actionGroup;
    }

    private List<AIModel> getProviderModels(DataContext dataContext) {
        ChatBoxForm chatBox = dataContext.getData(DataKeys.ASSISTANT_CHAT_BOX);
        if (chatBox == null) return Collections.emptyList();

        DBAIProfile profile = chatBox.getSelectedProfile();
        if (profile == null) return Collections.emptyList();

        return profile.getProvider().getModels();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        ChatBoxForm chatBox = e.getData(DataKeys.ASSISTANT_CHAT_BOX);
        boolean enabled = chatBox != null && chatBox.isPromptingAvailable();

        Presentation presentation = e.getPresentation();
        presentation.setText(getText(e));
        presentation.setDescription(txt("companion.chat.model.tooltip"));
        presentation.setEnabled(enabled);
    }

    private String getText(@NotNull AnActionEvent e) {
        ChatBoxForm chatBox = e.getData(DataKeys.ASSISTANT_CHAT_BOX);
        if (chatBox == null) return "Model";

        String text = getSelectedModelName(e);
        if (text != null) return text;

        List<AIModel> models = getProviderModels(e.getDataContext());
        if (!models.isEmpty()) return "Select Model";

        return "Model";
    }

    private static String getSelectedModelName(@NotNull AnActionEvent e) {
        ChatBoxForm chatBox = e.getData(DataKeys.ASSISTANT_CHAT_BOX);
        if (chatBox == null) return null;

        DBAIProfile profile = chatBox.getSelectedProfile();
        if (profile == null) return null;

        AIModel model = chatBox.getSelectedModel();
        if (model == null) return null;

        return Actions.adjustActionName(model.getId());
    }

    @Override
    protected boolean shouldShowDisabledActions() {
        return true;
    }
}
