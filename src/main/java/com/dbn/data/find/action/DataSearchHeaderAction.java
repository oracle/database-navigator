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

package com.dbn.data.find.action;


import com.dbn.common.action.BasicAction;
import com.dbn.data.find.DataSearchComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.actionSystem.Shortcut;
import lombok.Getter;

import javax.swing.JComponent;
import java.util.Set;

@Getter
public abstract class DataSearchHeaderAction extends BasicAction {
    private final DataSearchComponent searchComponent;

    protected DataSearchHeaderAction(DataSearchComponent searchComponent) {
        this.searchComponent = searchComponent;
    }

    protected static void registerShortcutsToComponent(Set<Shortcut> shortcuts, AnAction action, JComponent component) {
        CustomShortcutSet shortcutSet = new CustomShortcutSet(shortcuts.toArray(new Shortcut[0]));
        action.registerCustomShortcutSet(shortcutSet, component);
    }
}

