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

package com.dbn.assistant.tool.impl;

import com.dbn.assistant.tool.AssistantToolBase;
import com.dbn.assistant.tool.spec.UserPromptsTool;

public class UserPromptsToolImpl extends AssistantToolBase implements UserPromptsTool {

    @Override
    public boolean requestUserConfirmation(String title, String question, String yesOption, String noOption) {
        // return is irrelevant
        // (this proxy intercepted method is used for signature only - see AssistantPrompt)
        return false;
    }

    @Override
    public String requestUserDecision(String title, String question, String[] options) {
        // return is irrelevant
        // (this proxy intercepted method is used for signature only - see AssistantPrompt)
        return null;
    }
}
