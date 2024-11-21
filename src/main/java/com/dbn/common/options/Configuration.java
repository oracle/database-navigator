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

package com.dbn.common.options;

import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.nls.NlsSupport;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.dispose.Failsafe.nd;
import static com.dbn.common.util.Unsafe.cast;

public interface Configuration<P extends Configuration, E extends ConfigurationEditorForm>
        extends SearchableConfigurable, PersistentConfiguration, NlsSupport {

    @Nullable
    P getParent();

    @Nullable
    default <T> T getParentOfType(Class<T> type) {
        P parent = getParent();
        if (parent == null) return null;
        if (type.isAssignableFrom(parent.getClass())) return cast(parent);

        return cast(parent.getParentOfType(type));
    }

    @NotNull
    default P ensureParent() {
        return nd(getParent());
    }

    String getConfigElementName();

    @NotNull
    E createConfigurationEditor();

    E getSettingsEditor();

    E ensureSettingsEditor();

    Project resolveProject();
}
