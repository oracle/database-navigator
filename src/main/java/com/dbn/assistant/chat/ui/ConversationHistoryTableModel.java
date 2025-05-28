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

import com.dbn.assistant.chat.ChatConversation;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.list.FilteredList;
import com.dbn.common.locale.Formatter;
import com.dbn.common.ui.table.DBNReadonlyTableModel;
import com.dbn.common.ui.util.Listeners;
import com.dbn.common.util.Strings;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.util.Date;
import java.util.List;


public class ConversationHistoryTableModel extends StatefulDisposableBase implements DBNReadonlyTableModel<ChatConversation> {
    private final Listeners<TableModelListener> listeners = Listeners.create(this);
    private final ChatConversationFilter filter =  new ChatConversationFilter();
    private List<ChatConversation> conversations;
    private final Project project;

    public ConversationHistoryTableModel(Project project, List<ChatConversation> conversations) {
        super();
        this.conversations = FilteredList.stateful(filter, conversations);
        this.project = project;
    }

    @Override
    public int getRowCount() {
        return conversations.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @NonNls
    @Override
    public String getColumnName(int columnIndex) {
        switch (columnIndex) {
            case 0: return "Title";
            case 1: return "Profile";
            case 2: return "Date";
            default: return "";
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return ChatConversation.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return conversations.get(rowIndex);
    }

    @Override
    public Object getValue(ChatConversation conversation, int column) {
        switch (column) {
            case 0: return conversation.getTitle();
            case 1: return conversation.getContext().getProfile();
            case 2: return getPresentableDateFormat(conversation.getTimestamp());
            default: return "";
        }
    }

    @Override
    public String getPresentableValue(ChatConversation conversation, int column) {
        if (conversation == null) return "";
        switch (column) {
            case 0: return conversation.getTitle();
            case 1: return conversation.getContext().getProfile();
            case 2: return getPresentableDateFormat(conversation.getTimestamp());
            default: return "";
        }
    }

    private String getPresentableDateFormat(long timestamp) {
        Formatter formatter = Formatter.getInstance(project);
        Date date = new Date(timestamp);
        return formatter.formatDateTime(date);
    }

    /**
     * Apply a filter to the table data
     *
     * @param text The filter text
     */
    public void filter(String text) {
        if (!Strings.equalsIgnoreCase(text, filter.getText())) {
            filter.setText(text);
            notifyModelListeners();
        }
    }

    /**
     * Update the list of conversations
     *
     * @param conversations New list of conversations
     */
    public void setConversations(List<ChatConversation> conversations) {
        this.conversations = FilteredList.stateful(filter, conversations);
        notifyModelListeners();
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    private void notifyModelListeners() {
        TableModelEvent modelEvent = new TableModelEvent(this);
        listeners.notify(l -> l.tableChanged(modelEvent));
    }

    /**
     * Get the ID of a conversation at the specified row index
     *
     * @param rowIndex The row index
     * @return The conversation ID or null if out of bounds
     */
    @Nullable
    public String getIdAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < conversations.size()) {
            return conversations.get(rowIndex).getId();
        }
        return null;
    }

    @Override
    public void disposeInner() {
        nullify();
    }
}