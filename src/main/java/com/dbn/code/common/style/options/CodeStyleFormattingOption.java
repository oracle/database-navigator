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

package com.dbn.code.common.style.options;

import com.dbn.code.common.style.presets.CodeStylePreset;
import com.dbn.common.options.PersistentConfiguration;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.dbn.common.options.setting.Settings.stringAttribute;

@Getter
@Setter
@EqualsAndHashCode
public class CodeStyleFormattingOption implements PersistentConfiguration {
    private final Map<String, CodeStylePreset> presets = new LinkedHashMap<>();
    private String name;
    private String displayName;
    private CodeStylePreset preset;

    public CodeStyleFormattingOption(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    public void addPreset(CodeStylePreset preset) {
        presets.put(preset.getId(), preset);
    }

    public void addPreset(CodeStylePreset preset, boolean makeDefault) {
        presets.put(preset.getId(), preset);
        if (makeDefault) this.preset = preset;
    }

    public CodeStylePreset[] getPresets() {
        return presets.values().toArray(new CodeStylePreset[0]);
    }

    private CodeStylePreset getPreset(String id) {
        return presets.get(id);
    }

    /*********************************************************
     *                PersistentConfiguration                *
     *********************************************************/
    @Override
    public void readConfiguration(Element element) {
        name = stringAttribute(element, "name");
        String presetId = stringAttribute(element, "value");
        CodeStylePreset newPreset = getPreset(presetId);
        if (newPreset != null) preset = newPreset;
    }

    @Override
    public void writeConfiguration(Element element) {
        element.setAttribute("name", name);
        element.setAttribute("value", preset.getId());
    }
}