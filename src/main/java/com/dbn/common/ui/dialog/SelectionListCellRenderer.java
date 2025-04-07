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

package com.dbn.common.ui.dialog;

import com.dbn.common.ui.Presentable;
import com.dbn.common.ui.list.ColoredListCellRenderer;
import com.intellij.navigation.ItemPresentation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JList;

import static com.dbn.common.util.Strings.isNotEmpty;

public class SelectionListCellRenderer<T> extends ColoredListCellRenderer<T> {
    @Override
    protected void customize(@NotNull JList list, Object value, int index, boolean selected, boolean hasFocus) {
        if (value == null) return;

        String text = getText(value);
        Icon icon = getIcon(value);

        append(text);
        setIcon(icon);
    }

    private String getText(Object value) {
        if (value == null) return "";

        if (value instanceof ItemPresentation) {
            ItemPresentation presentation = (ItemPresentation) value;
            String text = presentation.getPresentableText();
            if (isNotEmpty(text)) return text;
        }

        if (value instanceof Presentable) {
            Presentable presentable = (Presentable) value;
            String text = presentable.getName();
            if (isNotEmpty(text)) return text;
        }

        return value.toString();
    }

    private @Nullable Icon getIcon(Object value) {
        if (value == null) return null;

        if (value instanceof Presentable) {
            Presentable presentable = (Presentable) value;
            Icon icon = presentable.getIcon();
            if (icon != null) return icon;
        }

        if (value instanceof ItemPresentation) {
            ItemPresentation presentation = (ItemPresentation) value;
            return presentation.getIcon(false);
        }

        return null;

    }
}
