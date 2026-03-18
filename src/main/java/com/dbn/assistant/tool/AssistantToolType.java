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

package com.dbn.assistant.tool;

import com.dbn.common.constant.PseudoConstant;
import com.dbn.common.constant.PseudoConstantConverter;
import org.jetbrains.annotations.NonNls;

public final class AssistantToolType extends PseudoConstant<AssistantToolType> {
    public static final AssistantToolType USER_PROMPTS = get("USER_PROMPTS");
    public static final AssistantToolType SEMANTIC_SEARCH = get("SEMANTIC_SEARCH");

    AssistantToolType(@NonNls String id) {
        super(id);
    }

    public static AssistantToolType get(@NonNls String id) {
        return PseudoConstant.get(AssistantToolType.class, id);
    }

    public static class Converter extends PseudoConstantConverter<AssistantToolType> {
        public Converter() {
            super(AssistantToolType.class);
        }
    }
}
