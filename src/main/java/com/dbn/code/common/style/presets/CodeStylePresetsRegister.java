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

package com.dbn.code.common.style.presets;

import java.util.HashMap;
import java.util.Map;

public class CodeStylePresetsRegister {
    private static final Map<String, CodeStylePreset> wrapPresets = new HashMap<>();

    public static void registerWrapPreset(CodeStylePreset codeStylePreset) {
        wrapPresets.put(codeStylePreset.getId(), codeStylePreset);
    }

    public static CodeStylePreset getWrapPreset(String id) {
        return wrapPresets.get(id);
    }
}
