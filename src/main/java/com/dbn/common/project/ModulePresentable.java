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

package com.dbn.common.project;

import com.dbn.common.ui.Presentable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ModulePresentable implements Presentable {
    private final ModuleRef module;

    public ModulePresentable(Module module) {
        this.module = ModuleRef.of(module);
    }

    public static List<ModulePresentable> fromModules(Module[] modules) {
        return Arrays.stream(modules).map(m -> new ModulePresentable(m)).collect(Collectors.toList());
    }

    @Nullable
    public Module getModule() {
        return ModuleRef.get(module);
    }

    @Override
    public @NotNull String getName() {
        Module module = getModule();
        return module == null ? "UNDEFINED" : module.getName();
    }

    @Override
    public @Nullable Icon getIcon() {
        Module module = getModule();
        if (module == null) return null;

        ModuleType<?> moduleType = ModuleType.get(module);
        return moduleType.getIcon();
    }
}
