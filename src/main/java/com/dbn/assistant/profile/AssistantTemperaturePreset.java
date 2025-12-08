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

@Getter
public enum AssistantTemperaturePreset implements Constant<AssistantTemperaturePreset>, Presentable {
    PRECISE("Precise", "Generate responses with high accuracy and minimal creativity. Ideal for tasks that require precise and factual information.", 0.0),
    BALANCED("Balanced", "Strike a balance between accuracy and creativity. Suitable for most tasks, providing a mix of reliability and innovation.", 0.2),
    EXPLORATORY("Exploratory", "Encourage more creative and diverse responses. Useful for tasks that benefit from exploring different ideas and perspectives.", 0.5),
    EXPERIMENTAL("Experimental", "Generate highly innovative and potentially unconventional responses. Ideal for tasks that require pushing the boundaries of language and creativity.", 0.9),
    CUSTOM("Custom", "Manually adjust the temperature to fine-tune the response generation. Allows for precise control over the balance between accuracy and creativity.", 0.5);

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
