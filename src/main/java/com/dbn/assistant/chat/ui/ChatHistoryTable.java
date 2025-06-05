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

package com.dbn.assistant.chat.ui;

import com.dbn.assistant.chat.Chat;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.table.DBNColoredTableCellRenderer;
import com.dbn.common.ui.table.DBNTable;
import com.intellij.ui.SimpleTextAttributes;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;

@Setter
public class ChatHistoryTable extends DBNTable<ChatHistoryTableModel> {
    private Runnable doubleClickAction;

    public ChatHistoryTable(@NotNull DBNComponent parent, List<Chat> chats) {
        super(parent, new ChatHistoryTableModel(parent.ensureProject(), chats), true);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setDefaultRenderer(Chat.class, new CellRenderer());
        initTableSorter();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && doubleClickAction != null) {
                    int selectedRow = getSelectedRow();
                    if (selectedRow >= 0) {
                        doubleClickAction.run();
                    }
                }
            }
        });

        setAccessibleName(this, "Chat History");
        setProportionalColumnWidths(50, 20, 30);
    }

    @Override
    public void setModel(@NotNull TableModel dataModel) {
        super.setModel(dataModel);
        initTableSorter();
    }

    private class CellRenderer extends DBNColoredTableCellRenderer {
        @Override
        protected void customizeCellRenderer(DBNTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
            Chat chat = (Chat) value;
            String columnValue = getModel().getPresentableValue(chat, column);
            append(columnValue == null ? "" : columnValue, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }
    }
}