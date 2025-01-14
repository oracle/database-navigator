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

package com.dbn.navigation.object;

import com.dbn.common.dispose.Failsafe;
import com.dbn.common.ui.list.ColoredListCellRenderer;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.common.DBObject;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.JList;

public class DBObjectListCellRenderer extends ColoredListCellRenderer {
    public static final DBObjectListCellRenderer INSTANCE = new DBObjectListCellRenderer();

    private DBObjectListCellRenderer() {}

    @Override
    protected void customize(@NotNull JList list, Object value, int index, boolean selected, boolean hasFocus) {
        if (value instanceof DBObject) {
            DBObject object = (DBObject) value;
            setIcon(object.getIcon());
            append(object.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            ConnectionHandler connection = Failsafe.nn(object.getConnection());
            append(" [" + connection.getName() + "]", SimpleTextAttributes.GRAY_ATTRIBUTES);
            if (object.getParentObject() != null) {
                append(" - " + object.getParentObject().getQualifiedName(), SimpleTextAttributes.GRAY_ATTRIBUTES);
            }
        } else {
            append(value.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }
    }
}
