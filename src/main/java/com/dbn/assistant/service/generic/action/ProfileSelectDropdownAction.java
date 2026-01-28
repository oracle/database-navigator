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
import com.dbn.assistant.chat.window.action.AssistantActionSupport;
import com.dbn.assistant.chat.window.ui.ChatBoxForm;
import com.dbn.assistant.profile.AssistantProfile;
import com.dbn.assistant.profile.DeclaredAssistantProfile;
import com.dbn.assistant.profile.ImplicitAssistantProfile;
import com.dbn.assistant.profile.PotentialAssistantProfile;
import com.dbn.common.action.BackgroundUpdate;
import com.dbn.common.action.ComboBoxAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.util.List;
import java.util.Objects;

import static com.dbn.assistant.chat.ChatAvailability.AVAILABLE;
import static com.dbn.assistant.chat.ChatAvailability.DISABLED_PROFILE_SELECTED;
import static com.dbn.assistant.chat.ChatAvailability.NO_PROFILE_AVAILABLE;
import static com.dbn.assistant.chat.ChatAvailability.NO_PROFILE_SELECTED;
import static com.dbn.assistant.profile.AssistantProfileLookup.getDeclaredProfiles;
import static com.dbn.assistant.profile.AssistantProfileLookup.getUndefinedImplicitProfiles;
import static com.dbn.assistant.profile.AssistantProfileLookup.getUndefinedPotentialProfiles;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.nls.NlsResources.txt;

@BackgroundUpdate
public class ProfileSelectDropdownAction extends ComboBoxAction implements AssistantActionSupport, DumbAware {
    private transient String selectedProfileId;

    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(@NotNull JComponent component, DataContext dataContext) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        Project project = dataContext.getData(PlatformDataKeys.PROJECT);
        if (project == null) return actionGroup;

        List<DeclaredAssistantProfile> declaredProfiles = getDeclaredProfiles(project);
        List<ImplicitAssistantProfile> implicitProfiles = getUndefinedImplicitProfiles(project);
        List<PotentialAssistantProfile> potentialProfiles = getUndefinedPotentialProfiles(project);

        addProfileActions(actionGroup, declaredProfiles);
        addProfileActions(actionGroup, implicitProfiles);

        boolean emptyStup = declaredProfiles.isEmpty() && implicitProfiles.isEmpty();

        if (emptyStup) {
            addProfileActions(actionGroup, potentialProfiles);
        } else {
            // group new profiles
            actionGroup.addSeparator();
            DefaultActionGroup newProvidersGroup = new DefaultActionGroup("New", true);
            actionGroup.add(newProvidersGroup);
            addProfileActions(newProvidersGroup, potentialProfiles);
        }


        return actionGroup;
    }

    @Override
    protected Condition<AnAction> getPreselectCondition() {
        return a -> {
            if (a instanceof ProfileSelectAction profileAction) {
                return Objects.equals(profileAction.getProfileId(), selectedProfileId);
            }
            return false;
        };
    }

    private static void addProfileActions(DefaultActionGroup actionGroup, List<? extends AssistantProfile> profiles) {
        if (profiles.isEmpty()) return;

        profiles.forEach(p -> actionGroup.add(new ProfileSelectAction(p)));
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        boolean enabled = isEnabled(e);

        Presentation presentation = e.getPresentation();
        presentation.setText(getText(e), false);
        presentation.setDescription(txt("app.assistant.tooltip.ChooseProfile"));
        presentation.setEnabled(enabled);
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
        // update transient selected profile when action presentation is updated
        AssistantProfile selectedProfile = getSelectedProfile(e);
        selectedProfileId = selectedProfile == null ? null : selectedProfile.getId();

        ChatBoxForm chatBox = getChatBox(e);
        if (chatBox == null) return txt("app.assistant.action.Profile");

        String text = getSelectedProfileName(e);
        if (isNotEmpty(text)) return text;

        return txt("app.assistant.action.Profile");
    }

    @Override
    protected boolean shouldShowDisabledActions() {
        return true;
    }
}
