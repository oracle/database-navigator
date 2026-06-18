/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.common.state;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;

@UtilityClass
public class StateEncryptionScopes {
    public static final @NonNls String ASSISTANT_CHAT_MESSAGE_CONTENT = "assistant.chat.message.content";
    public static final @NonNls String ASSISTANT_TOOL_ARGUMENTS = "assistant.tool.arguments";
    public static final @NonNls String ASSISTANT_TOOL_RESPONSE = "assistant.tool.response";
    public static final @NonNls String EXECUTION_VARIABLE_VALUE = "execution.variable.value";
    public static final @NonNls String EXECUTION_VARIABLE_EXPRESSION = "execution.variable.expression";
    public static final @NonNls String EXECUTION_STATEMENT_VARIABLE = "execution.statement.variable";
}
