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

import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.locale.Formatter;
import com.dbn.common.ui.table.DBNReadonlyTableModel;
import com.dbn.common.ui.util.Listeners;
import com.dbn.assistant.chat.PersistentChatConversation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class ConversationHistoryTableModel extends StatefulDisposableBase implements DBNReadonlyTableModel<PersistentChatConversation> {
    private List<PersistentChatConversation> conversations;
    private final Listeners<TableModelListener> listeners = Listeners.create(this);
    private String filterText = "";
    private final Project project;
    public ConversationHistoryTableModel(Project project, List<PersistentChatConversation> conversations) {
        super();
        this.conversations = conversations.stream().filter(conv -> conv.getTitle() != null && !conv.getTitle().isEmpty()).collect(Collectors.toList());
        this.project = project;
    }

    @Override
    public int getRowCount() {
        return getFilteredConversations().size();
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
        return PersistentChatConversation.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return getFilteredConversations().get(rowIndex);
    }

    @Override
    public Object getValue(PersistentChatConversation conversation, int column) {

        switch (column) {
            case 0: return conversation.getTitle();
            case 1: return conversation.getContext().getProfile();
            case 2: return getPresentableDateFormat(conversation.getTimestamp());
            default: return "";
        }
    }

    @Override
    public String getPresentableValue(PersistentChatConversation conversation, int column) {
        if (conversation == null) return "";
        switch (column) {
            case 0: return conversation.getTitle();
            case 1: return conversation.getContext().getProfile();
            case 2: return getPresentableDateFormat(conversation.getTimestamp());
            default: return "";
        }
    }

    public String getPresentableDateFormat(long timestamp) {
        Formatter formatter = Formatter.getInstance(project);
        Date date = new Date(timestamp);
        return formatter.formatDateTime(date);
    }

    /**
     * Apply a filter to the table data
     *
     * @param text The filter text
     * @return True if the filter changed and the table should be updated
     */
    public boolean filter(String text) {
        if (text == null) {
            text = "";
        }

        if (!filterText.equals(text)) {
            filterText = text;
            TableModelEvent modelEvent = new TableModelEvent(this);
            listeners.notify(l -> l.tableChanged(modelEvent));
            return true;
        }
        return false;
    }

    /**
     * Get the list of conversations filtered by the current filter text
     *
     * @return Filtered list of conversations
     */
    public List<PersistentChatConversation> getFilteredConversations() {
        if (filterText.isEmpty()) {
            return conversations;
        }

        String lowerFilter = filterText.toLowerCase();
        return conversations.stream()
                .filter(conversation ->
                        conversation.getTitle().toLowerCase().contains(lowerFilter) ||
                                conversation.getId().toString().toLowerCase().contains(lowerFilter))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Update the list of conversations
     *
     * @param conversations New list of conversations
     */
    public void setConversations(List<PersistentChatConversation> conversations) {
        this.conversations = new ArrayList<>(conversations);
        TableModelEvent modelEvent = new TableModelEvent(this);
        listeners.notify(l -> l.tableChanged(modelEvent));
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    /**
     * Get the ID of a conversation at the specified row index
     *
     * @param rowIndex The row index
     * @return The conversation ID or null if out of bounds
     */
    @Nullable
    public Object getIdAt(int rowIndex) {
        List<PersistentChatConversation> filtered = getFilteredConversations();
        if (rowIndex >= 0 && rowIndex < filtered.size()) {
            return filtered.get(rowIndex).getId();
        }
        return null;
    }

    @Override
    public void disposeInner() {
        nullify();
    }
}