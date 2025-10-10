/*
 * Copyright 2025 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.assistant.service.generic.action;

import com.dbn.assistant.chat.ChatAvailability;
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.chat.window.action.AssistantActionSupport;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.assistant.profile.AssistantProfile;
import com.dbn.assistant.provider.AIModel;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.ComboBoxAction;
import com.dbn.common.util.Lists;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.util.List;
import java.util.Objects;

import static com.dbn.assistant.chat.ChatAvailability.AVAILABLE;
import static com.dbn.assistant.profile.AssistantProfileLookup.getProfile;
import static com.dbn.nls.NlsResources.txt;
import static java.util.Collections.emptyList;

@BackgroundUpdate
public class ModelSelectDropdownAction extends ComboBoxAction implements AssistantActionSupport, DumbAware {
    private transient String selectedModelId;

    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(@NotNull JComponent component, @NotNull DataContext dataContext) {
        List<AIModel> models = getModels(dataContext);

        DefaultActionGroup actionGroup = new DefaultActionGroup();
        Lists.forEach(models, m -> actionGroup.add(new ModelSelectAction(m)));

        return actionGroup;
    }

    @Override
    protected Condition<AnAction> getPreselectCondition() {
        return a -> {
            if (a instanceof ModelSelectAction) {
                ModelSelectAction modelAction = (ModelSelectAction) a;
                return Objects.equals(modelAction.getModelId(), selectedModelId);
            }
            return false;
        };
    }



    private List<AIModel> getModels(DataContext dataContext) {
        ChatContext context = getCurrentChatContext(dataContext);
        if (context == null) return emptyList();

        Project project = getProject(dataContext);
        if (project == null) return emptyList();

        String profileId = context.getProfileId();
        AssistantProfile profile = getProfile(project, profileId);
        if (profile == null) return emptyList();

        AIProvider provider = profile.getProvider();
        if (provider == null) return emptyList();

        return provider.getModels();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        boolean enabled = isEnabled(e);

        Presentation presentation = e.getPresentation();
        presentation.setText(getText(e), false);
        presentation.setDescription(txt("app.assistant.tooltip.ChooseModel"));
        presentation.setEnabled(enabled);
    }

    private boolean isEnabled(@NotNull AnActionEvent e) {
        ChatAvailability availability = getChatAvailability(e);
        return availability == AVAILABLE;
    }

    private String getText(@NotNull AnActionEvent e) {
        // update transient selected model when action presentation is updated
        AIModel model = getSelectedModel(e);
        selectedModelId = model == null ? null : model.getId();

        ChatBoxForm chatBox = getChatBox(e);
        if (chatBox == null) return txt("app.assistant.action.Model");

        String text = model == null ? null : model.getShortName();
        if (text != null) return text;

        return txt("app.assistant.action.Model");
    }

    @Override
    protected boolean shouldShowDisabledActions() {
        return true;
    }
}
