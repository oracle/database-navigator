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

package com.dbn.assistant.provider;

import com.dbn.assistant.AssistantType;
import org.jetbrains.annotations.NonNls;

public class AIProviders {
    public static final AIProvider ANTHROPIC = forId("ANTHROPIC");
    public static final AIProvider COHERE = forId("COHERE");
    public static final AIProvider GOOGLE = forId("GOOGLE");
    public static final AIProvider HUGGING_FACE = forId("HUGGING_FACE");
    public static final AIProvider OLLAMA = forId("OLLAMA");
    public static final AIProvider BEDROCK = forId("BEDROCK");
    public static final AIProvider OPEN_AI = forId("OPEN_AI");
    public static final AIProvider MISTRAL_AI = forId("MISTRAL_AI");
    public static final AIProvider OCI_GEN_AI = forId("OCI_GEN_AI");

    private static AIProvider forId(@NonNls String id) {
        return AIProviderData.getProvider(AssistantType.PUBLIC, id);
    }
}
