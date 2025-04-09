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

import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.table.DBNColoredTableCellRenderer;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.ui.table.DBNTableTransferHandler;
import com.dbn.assistant.chat.PersistentChatConversation;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;

public class ConversationHistoryTable extends DBNTable<ConversationHistoryTableModel> {
    private Runnable doubleClickAction;

    public ConversationHistoryTable(@NotNull DBNComponent parent, List<PersistentChatConversation> conversations) {
        super(parent, new ConversationHistoryTableModel(parent.ensureProject(), conversations), true);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        setDefaultRenderer(PersistentChatConversation.class, new CellRenderer());
        setTransferHandler(DBNTableTransferHandler.INSTANCE);
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

        setAccessibleName(this, "Chat Conversation History");
    }

    public void setDoubleClickAction(Runnable action) {
        this.doubleClickAction = action;
    }

    @Override
    public void setModel(@NotNull TableModel dataModel) {
        super.setModel(dataModel);
        initTableSorter();
    }

    /**
     * Adjusts the column widths to fit the content
     */
    public void adjustColumnWidths() {
        if (getColumnCount() > 0) {
            getColumnModel().getColumn(0).setPreferredWidth(350);
        }

        if (getColumnCount() > 1) {
            getColumnModel().getColumn(1).setPreferredWidth(150);
        }
    }

    private class CellRenderer extends DBNColoredTableCellRenderer {
        @Override
        protected void customizeCellRenderer(DBNTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
            PersistentChatConversation conversation = (PersistentChatConversation) value;
            String columnValue = getModel().getPresentableValue(conversation, column);
            append(columnValue == null ? "" : columnValue, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }
    }
}