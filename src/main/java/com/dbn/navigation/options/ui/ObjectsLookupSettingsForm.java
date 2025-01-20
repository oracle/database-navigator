/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.navigation.options.ui;

import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.ui.Presentable;
import com.dbn.common.ui.list.CheckBoxList;
import com.dbn.common.ui.util.Keyboard;
import com.dbn.navigation.options.ObjectsLookupSettings;
import com.dbn.nls.NlsResources;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.options.ConfigurationException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;

public class ObjectsLookupSettingsForm extends ConfigurationEditorForm<ObjectsLookupSettings> {
    private JPanel mainPanel;
    private JComboBox<ConnectionOption> connectionComboBox;
    private JComboBox<BehaviorOption> behaviorComboBox;
    private CheckBoxList<ObjectsLookupSettings.ObjectTypeEntry> lookupObjectsList;

    public ObjectsLookupSettingsForm(ObjectsLookupSettings configuration) {
        super(configuration);
        Shortcut[] shortcuts = Keyboard.getShortcuts("DBNavigator.Actions.Navigation.GotoDatabaseObject");
        TitledBorder border = (TitledBorder) mainPanel.getBorder();
        border.setTitle(txt("cfg.lookup.title.LookupObjects", KeymapUtil.getShortcutsText(shortcuts)));

        initComboBox(connectionComboBox,
                ConnectionOption.PROMPT,
                ConnectionOption.RECENT);

        initComboBox(behaviorComboBox,
                BehaviorOption.LOOKUP,
                BehaviorOption.LOAD);

        lookupObjectsList.setElements(configuration.getLookupObjectTypes());

        resetFormChanges();
        registerComponents(mainPanel);
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        lookupObjectsList.applyChanges();
        ObjectsLookupSettings configuration = getConfiguration();
        configuration.getForceDatabaseLoad().setValue(getSelection(behaviorComboBox).getValue());
        configuration.getPromptConnectionSelection().setValue(getSelection(connectionComboBox).getValue());
    }

    @Override
    public void resetFormChanges() {
        ObjectsLookupSettings configuration = getConfiguration();
        if (configuration.getForceDatabaseLoad().getValue())
            setSelection(behaviorComboBox, BehaviorOption.LOAD); else
            setSelection(behaviorComboBox, BehaviorOption.LOOKUP);

        if (configuration.getPromptConnectionSelection().getValue())
            setSelection(connectionComboBox, ConnectionOption.PROMPT); else
            setSelection(connectionComboBox, ConnectionOption.RECENT);
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    @Getter
    @AllArgsConstructor
    private enum ConnectionOption implements Presentable {
        PROMPT(NlsResources.txt("cfg.lookup.const.ConnectionOption_PROMPT"), true),
        RECENT(NlsResources.txt("cfg.lookup.const.ConnectionOption_RECENT"), false);

        private final String name;
        private final Boolean value;
    }

    @Getter
    @AllArgsConstructor
    private enum BehaviorOption implements Presentable {
        LOOKUP(NlsResources.txt("cfg.lookup.const.BehaviorOption_LOOKUP"), false),
        LOAD(NlsResources.txt("cfg.lookup.const.BehaviorOption_LOAD"), true);

        private final String name;
        private final Boolean value;
    }
}
