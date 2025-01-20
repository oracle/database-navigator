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
import com.intellij.openapi.options.ConfigurationException;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.swing.JTextField;

public class IntegerSetting extends Setting<Integer, JTextField> implements PersistentConfiguration {
    public IntegerSetting(@NonNls String name, Integer value) {
        super(name, value);
    }
    
    @Override
    public void readConfiguration(Element parent) {
        setValue(Settings.getInteger(parent, getName(), this.value()));
    }

    @Override
    public void writeConfiguration(Element parent) {
        Settings.setInteger(parent, getName(), this.value());
    }

    @Override
    public boolean to(JTextField component) throws ConfigurationException {
        return setValue(Integer.parseInt(component.getText()));
    }

    @Override
    public void from(JTextField component) {
        component.setText(value().toString());
    }

}
