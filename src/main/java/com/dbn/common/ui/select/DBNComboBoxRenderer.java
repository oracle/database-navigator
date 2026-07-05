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

package com.dbn.common.ui.select;

import com.dbn.common.ui.Presentable;
import com.dbn.common.ui.list.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComboBox;
import javax.swing.JList;

import static com.dbn.common.ui.util.ClientProperty.LOADING;
import static com.dbn.nls.NlsResources.txt;

public class DBNComboBoxRenderer<T> extends ColoredListCellRenderer<T> {
    private final JComboBox<T> comboBox;

    public DBNComboBoxRenderer(JComboBox<T> comboBox) {
        this.comboBox = comboBox;
    }

    @Override
    protected void customize(@NotNull JList<? extends T> list, T value, int index, boolean selected, boolean hasFocus) {
        if (value != null) {
            if (value instanceof Presentable presentable) {
                append(presentable.getName());
                setIcon(presentable.getIcon());
            } else {
                append(value.toString());
            }
            return;
        }
        boolean loading = LOADING.is(comboBox);
        if (loading) {
            append(txt("app.shared.placeholder.Loading"), SimpleTextAttributes.GRAY_ATTRIBUTES);
        }
    }
}
