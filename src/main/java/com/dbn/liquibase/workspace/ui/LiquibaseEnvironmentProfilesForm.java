/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.liquibase.workspace.ui;

import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.environment.EnvironmentTypeId;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Strings;
import com.dbn.liquibase.workspace.LiquibaseEnvironmentProfile;
import com.dbn.liquibase.workspace.LiquibaseEnvironmentProfileBundle;
import com.intellij.ui.ToolbarDecorator;
import org.jetbrains.annotations.NotNull;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.Map;

import static com.dbn.common.ui.CardLayouts.addCard;
import static com.dbn.common.ui.CardLayouts.getCard;
import static com.dbn.common.ui.CardLayouts.removeCard;
import static com.dbn.common.ui.CardLayouts.showCard;
import static com.dbn.common.ui.util.Decorators.createToolbarDecorator;
import static com.dbn.common.ui.util.Decorators.createToolbarDecoratorComponent;
import static com.dbn.nls.NlsResources.txt;

/** List and detail form for managing named Liquibase environment profiles. */
public class LiquibaseEnvironmentProfilesForm extends DBNFormBase {
    private static final String EMPTY_CARD = "DBN_LIQUIBASE_EMPTY_ENVIRONMENT_PROFILES";
    private JPanel mainPanel;
    private JPanel profilesPanel;
    private JPanel detailsPanel;
    private JList<LiquibaseEnvironmentProfile> profilesList;

    private final LiquibaseEnvironmentProfileBundle bundle;
    private final Map<String, LiquibaseEnvironmentProfileForm> profileForms = DisposableContainers.map(this);

    LiquibaseEnvironmentProfilesForm(@NotNull LiquibaseEnvironmentProfilesDialog parent) {
        super(parent);
        bundle = parent.getBundle();
        profilesList.setCellRenderer((list, value, index, selected, focus) -> {
            String name = Strings.isEmpty(value.getName()) ? txt("app.shared.placeholder.Unnamed") : value.getName();
            JLabel label = new JLabel(name);
            label.setOpaque(true);
            label.setBackground(selected ? list.getSelectionBackground() : list.getBackground());
            label.setForeground(selected ? list.getSelectionForeground() : list.getForeground());
            return label;
        });
        profilesList.addListSelectionListener(e -> showSelectedProfile());
        profilesPanel.removeAll();
        profilesPanel.add(initProfilesList());
        initDetailsPanel();
        updateProfiles();
        if (profilesList.getModel().getSize() > 0) {
            profilesList.setSelectedIndex(0);
        } else {
            showSelectedProfile();
        }
    }

    private void initDetailsPanel() {
        addCard(detailsPanel, createEmptyDetails(), EMPTY_CARD);
    }

    private JComponent createEmptyDetails() {
        DBNHintForm hintForm = new DBNHintForm(this, TextContent.plain(txt("app.liquibase.hint.NoEnvironmentProfiles")), null, false);
        JPanel hintPanel = new JPanel(new BorderLayout());
        hintPanel.setBorder(Borders.insetBorder(0, 8,8,8));
        hintPanel.add(hintForm.getComponent());
        return hintPanel;
    }

    private JPanel initProfilesList() {
        ToolbarDecorator decorator = createToolbarDecorator(profilesList);
        decorator.setAddAction(button -> addProfile());
        decorator.setRemoveAction(button -> removeProfile());
        return createToolbarDecoratorComponent(decorator, profilesList);
    }

    private void updateProfiles() {
        DefaultListModel<LiquibaseEnvironmentProfile> model = new DefaultListModel<>();
        bundle.getProfiles().forEach(model::addElement);
        profilesList.setModel(model);
    }

    void refreshProfileList() {
        profilesList.repaint();
    }

    private void showSelectedProfile() {
        LiquibaseEnvironmentProfile profile = profilesList.getSelectedValue();
        if (profile == null) {
            showCard(detailsPanel, EMPTY_CARD);
            return;
        }
        DBNForm form = profileForms.computeIfAbsent(profile.getId(), id ->
                new LiquibaseEnvironmentProfileForm(this, profile, true));
        if (getCard(detailsPanel, profile.getId()) == null) {
            addCard(detailsPanel, form, profile.getId());
        }
        showCard(detailsPanel, profile.getId());
    }

    private void addProfile() {
        LiquibaseEnvironmentProfile profile = bundle.createProfile(
                txt("app.liquibase.placeholder.NewEnvironmentProfile"), EnvironmentTypeId.DEFAULT);
        updateProfiles();
        profilesList.setSelectedValue(profile, true);
        markFormChanged();
    }

    private void removeProfile() {
        LiquibaseEnvironmentProfile profile = profilesList.getSelectedValue();
        if (profile == null) return;
        int selectedIndex = profilesList.getSelectedIndex();
        bundle.removeProfile(profile.getId());
        profileForms.remove(profile.getId());
        removeCard(detailsPanel, profile.getId());
        updateProfiles();
        int nextIndex = Math.min(selectedIndex, profilesList.getModel().getSize() - 1);
        if (nextIndex >= 0) {
            profilesList.setSelectedIndex(nextIndex);
        } else {
            showSelectedProfile();
        }
        markFormChanged();
    }

    public void applyFormChanges() {
        profileForms.values().forEach(LiquibaseEnvironmentProfileForm::applyFormChanges);
    }

    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return profilesList;
    }
}
