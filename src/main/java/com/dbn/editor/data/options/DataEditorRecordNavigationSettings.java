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

package com.dbn.editor.data.options;

import com.dbn.common.options.BasicConfiguration;
import com.dbn.common.options.setting.Settings;
import com.dbn.data.record.navigation.RecordNavigationTarget;
import com.dbn.editor.data.options.ui.DataEditorRecordNavigationSettingsForm;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class DataEditorRecordNavigationSettings extends BasicConfiguration<DataEditorSettings, DataEditorRecordNavigationSettingsForm> {
    private RecordNavigationTarget navigationTarget = RecordNavigationTarget.VIEWER;

    public DataEditorRecordNavigationSettings(DataEditorSettings parent) {
        super(parent);
    }

    @NotNull
    @Override
    public DataEditorRecordNavigationSettingsForm createConfigurationEditor() {
        return new DataEditorRecordNavigationSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "record-navigation";
    }

    @Override
    public void readConfiguration(Element element) {
        navigationTarget = Settings.getEnum(element, "navigation-target", RecordNavigationTarget.VIEWER);
        if (navigationTarget == RecordNavigationTarget.PROMPT) {
            navigationTarget = RecordNavigationTarget.ASK;
        }
    }

    @Override
    public void writeConfiguration(Element element) {
        Settings.setEnum(element, "navigation-target", navigationTarget);
    }
}
