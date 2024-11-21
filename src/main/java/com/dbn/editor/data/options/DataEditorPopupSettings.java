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
import com.dbn.editor.data.options.ui.DataEditorPopupSettingsForm;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class DataEditorPopupSettings extends BasicConfiguration<DataEditorSettings, DataEditorPopupSettingsForm> {
    private boolean active = false;
    private boolean activeIfEmpty = false;
    private int dataLengthThreshold = 100;
    private int delay = 1000;

    public DataEditorPopupSettings(DataEditorSettings parent) {
        super(parent);
    }

    @Override
    public String getDisplayName() {
        return txt("cfg.dataEditor.title.EditorPopups");
    }

    /****************************************************
     *                   Configuration                  *
     ****************************************************/
   @Override
   @NotNull
   public DataEditorPopupSettingsForm createConfigurationEditor() {
       return new DataEditorPopupSettingsForm(this);
   }

    @Override
    public String getConfigElementName() {
        return "text-editor-popup";
    }

    @Override
    public void readConfiguration(Element element) {
        active = Settings.getBoolean(element, "active", active);
        activeIfEmpty = Settings.getBoolean(element, "active-if-empty", activeIfEmpty);
        dataLengthThreshold = Settings.getInteger(element, "data-length-threshold", dataLengthThreshold);
        delay = Settings.getInteger(element, "popup-delay", delay);
    }

    @Override
    public void writeConfiguration(Element element) {
        Settings.setBoolean(element, "active", active);
        Settings.setBoolean(element, "active-if-empty", activeIfEmpty);
        Settings.setInteger(element, "data-length-threshold", dataLengthThreshold);
        Settings.setInteger(element, "popup-delay", delay);
    }

}
