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

package com.dbn.assistant.profile;

import com.dbn.common.constant.Constant;
import com.dbn.common.ui.Presentable;
import lombok.Getter;

import static com.dbn.nls.NlsResources.txt;

@Getter
public enum AssistantTemperaturePreset implements Constant<AssistantTemperaturePreset>, Presentable {
    PRECISE(
        txt("app.assistant.const.AssistantTemperaturePreset_PRECISE"),
        txt("app.assistant.hint.AssistantTemperaturePreset_PRECISE"), 0.0),

    BALANCED(
        txt("app.assistant.const.AssistantTemperaturePreset_BALANCED"),
        txt("app.assistant.hint.AssistantTemperaturePreset_BALANCED"), 0.2),

    EXPLORATORY(
        txt("app.assistant.const.AssistantTemperaturePreset_EXPLORATORY"),
        txt("app.assistant.hint.AssistantTemperaturePreset_EXPLORATORY"), 0.5),

    EXPERIMENTAL(
        txt("app.assistant.const.AssistantTemperaturePreset_EXPERIMENTAL"),
        txt("app.assistant.hint.AssistantTemperaturePreset_EXPERIMENTAL"), 0.9),

    CUSTOM(
        txt("app.assistant.const.AssistantTemperaturePreset_CUSTOM"),
        txt("app.assistant.hint.AssistantTemperaturePreset_CUSTOM"), 0.5);

    ;

    private final String name;
    private final String description;
    private final double value;

    AssistantTemperaturePreset(String name, String description, double value) {
        this.name = name;
        this.description = description;
        this.value = value;
    }
}
