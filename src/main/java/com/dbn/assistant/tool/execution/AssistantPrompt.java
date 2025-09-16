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

package com.dbn.assistant.tool.execution;

import com.dbn.common.data.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class AssistantPrompt {
    private final String title;
    private final String message;
    private final List<String> options;

    public AssistantPrompt(AssistantToolRequest request) {
        List<?> values = request.getArgumentValues();
        title = Data.asString(values.get(0));
        message = Data.asString(values.get(1));
        options = new ArrayList<>();
        for (int i=2; i<values.size(); i++) {
            Object o = values.get(i);
            options.addAll(Data.asStringList(o));
        }
    }
}
