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

package com.dbn.assistant.tool.feature;

import com.dbn.assistant.tool.AssistantToolType;
import com.dbn.common.extension.ExtensionPoint;
import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Extension point for adding feature buttons to assistant tool sections.
 */
public interface AssistantToolFeature extends ExtensionPoint {
    ExtensionPointName<AssistantToolFeature> EP = ExtensionPointName.create("com.dbn.assistantToolFeature");

    /**
     * Returns the text displayed on the feature button.
     */
    @NotNull
    String getName();

    /**
     * Indicates whether this feature applies to the given tool type and tool name.
     */
    default boolean supports(@NotNull AssistantToolType toolType, @NotNull String toolName) {
        return true;
    }

    /**
     * Returns an additional approval timeout for tools supported by this feature.
     */
    default @NotNull Duration getApprovalTimeoutExtension() {
        return Duration.ZERO;
    }

    /**
     * Executes this feature for the given tool request context.
     */
    void execute(@NotNull AssistantToolFeatureContext context);
}
