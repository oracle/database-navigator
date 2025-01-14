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

package com.dbn.object.common.ui;

import com.dbn.common.ui.list.ColoredListCellRenderer;
import com.dbn.object.common.DBSchemaObject;
import org.jetbrains.annotations.NotNull;

import javax.swing.JList;
import javax.swing.ListCellRenderer;

import static com.intellij.ui.SimpleTextAttributes.GRAY_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES;

public class DBObjectListCellRenderer<T extends DBSchemaObject> extends ColoredListCellRenderer<T> {
    public static <T extends DBSchemaObject> ListCellRenderer<T> create() {
        return new DBObjectListCellRenderer<>();
    }

    @Override
    protected void customize(@NotNull JList<? extends T> list, T object, int index, boolean selected, boolean hasFocus) {
        String objectName = object.getName();
        boolean enabled = list.isEnabled() && object.isEnabled();
        append(objectName, enabled  ? REGULAR_ATTRIBUTES : GRAY_ATTRIBUTES);
        setIcon(object.getIcon());
    }
}
