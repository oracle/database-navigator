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

package com.dbn.object.common.ui;

import com.dbn.common.ui.ValueSelectorOption;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.select.DBNComboBoxRenderer;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;

import javax.swing.JList;

public class DBObjectTypeSelector extends DBNComboBox<DBObjectType> {
    public DBObjectTypeSelector() {
        set(ValueSelectorOption.HIDE_ICON, true);
        setRenderer(new DBNComboBoxRenderer<>(this) {
            @Override
            protected void customize(@NotNull JList<? extends DBObjectType> list, DBObjectType value,
                                     int index, boolean selected, boolean hasFocus) {
                if (value == null) {
                    super.customize(list, null, index, selected, hasFocus);
                } else {
                    append(value.getTitleCasedDisplayName());
                }
            }
        });
    }

    @Override
    public String getOptionDisplayName(DBObjectType value) {
        return value == null ? "" : value.getTitleCasedDisplayName();
    }
}
