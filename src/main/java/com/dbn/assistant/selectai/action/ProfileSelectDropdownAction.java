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

package com.dbn.assistant.selectai.action;

import com.dbn.assistant.chat.ChatAvailability;
import com.dbn.assistant.chat.window.action.AssistantActionSupport;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.assistant.selectai.SelectAiContextUtil;
import com.dbn.common.action.ComboBoxAction;
import com.dbn.common.action.DataKeys;
import com.dbn.connection.ConnectionId;
import com.dbn.object.DBAIProfile;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.util.List;

import static com.dbn.assistant.chat.ChatAvailability.AVAILABLE;
import static com.dbn.assistant.chat.ChatAvailability.DISABLED_PROFILE_SELECTED;
import static com.dbn.assistant.chat.ChatAvailability.NO_PROFILE_AVAILABLE;
import static com.dbn.assistant.chat.ChatAvailability.NO_PROFILE_SELECTED;
import static com.dbn.nls.NlsResources.txt;

/**
 * Action for selecting the current AI-assistant profile
 *
 * @author Dan Cioca (Oracle)
 */
public class ProfileSelectDropdownAction extends ComboBoxAction implements AssistantActionSupport, DumbAware {

    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(JComponent component, DataContext dataContext) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        ChatBoxForm chatBox = dataContext.getData(DataKeys.ASSISTANT_CHAT_BOX);
        if (chatBox == null) return actionGroup;

        List<DBAIProfile> profiles = SelectAiContextUtil.getProfiles(chatBox.getConnectionId());
        profiles.forEach(p -> actionGroup.add(new ProfileSelectAction(p)));
        actionGroup.addSeparator();

        actionGroup.add(new ProfileCreateAction());
        return actionGroup;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        boolean enabled = isEnabled(e);

        DBAIProfile profile = getSelectedProfile(e);

        Presentation presentation = e.getPresentation();
        presentation.setText(getText(e), false);
        presentation.setDescription(txt("app.assistant.tooltip.ChooseProfile"));
        presentation.setEnabled(enabled);
        presentation.setIcon(profile == null ? null : profile.getIcon());
    }

    private boolean isEnabled(@NotNull AnActionEvent e) {
        ChatAvailability availability = getChatAvailability(e);
        return availability.isOneOf(
                AVAILABLE,
                NO_PROFILE_AVAILABLE,
                NO_PROFILE_SELECTED,
                DISABLED_PROFILE_SELECTED);
    }

    private String getText(@NotNull AnActionEvent e) {
        ChatBoxForm chatBox = getChatBox(e);
        if (chatBox == null) return txt("app.assistant.action.Profile");

        String text = getSelectedProfileName(e);
        if (text != null) return text;

        return txt("app.assistant.action.Profile");
    }

    @Nullable
    private String getSelectedProfileName(@NotNull AnActionEvent e) {
        DBAIProfile profile = getSelectedProfile(e);
        if (profile == null) return null;

        return profile.getName();
    }

    @Nullable
    private DBAIProfile getSelectedProfile(@NotNull AnActionEvent e) {
        ConnectionId connectionId = getConnectionId(e);
        if (connectionId == null) return null;

        return SelectAiContextUtil.getSelectedProfile(connectionId);
    }

    @Override
    protected boolean shouldShowDisabledActions() {
        return true;
    }
}
