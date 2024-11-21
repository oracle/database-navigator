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

package com.dbn.debugger.common.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.xdebugger.settings.DebuggerSettingsCategory;
import com.intellij.xdebugger.settings.XDebuggerSettings;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

public class DBProgramDebuggerSettings extends XDebuggerSettings<DBProgramDebuggerState> {
    DBProgramDebuggerState state = new DBProgramDebuggerState();

    protected DBProgramDebuggerSettings() {
        super("db-program");
    }

    @Override
    public @NotNull Collection<? extends Configurable> createConfigurables(@NotNull DebuggerSettingsCategory category) {
        if (category == DebuggerSettingsCategory.ROOT) {
            return Collections.singleton(new DBProgramDebuggerConfigurable());
        }
        return super.createConfigurables(category);
    }

    @Override
    public DBProgramDebuggerState getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull DBProgramDebuggerState state) {
        this.state = state;
    }

}
