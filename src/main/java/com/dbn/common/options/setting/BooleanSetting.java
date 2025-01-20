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

package com.dbn.common.options.setting;

import com.dbn.common.options.PersistentConfiguration;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.swing.JToggleButton;

public class BooleanSetting extends Setting<Boolean, JToggleButton> implements PersistentConfiguration {
    public BooleanSetting(@NonNls String name, Boolean value) {
        super(name, value);
    }
    
    @Override
    public void readConfiguration(Element parent) {
        setValue(Settings.getBoolean(parent, getName(), this.value()));
    }

    public void readConfigurationAttribute(Element parent) {
        setValue(Settings.booleanAttribute(parent, getName(), this.value()));
    }

    @Override
    public void writeConfiguration(Element parent) {
        Settings.setBoolean(parent, getName(), this.value());
    }

    public void writeConfigurationAttribute(Element parent) {
        Settings.setBooleanAttribute(parent, getName(), this.value());
    }


    @Override
    public boolean to(JToggleButton checkBox) {
        return setValue(checkBox.isSelected());
    }
    
    @Override
    public void from(JToggleButton checkBox) {
        checkBox.setSelected(value());
    }
}
