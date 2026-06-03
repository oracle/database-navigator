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

package com.dbn.assistant.service.generic.action;

import com.dbn.assistant.AssistantMode;
import com.dbn.assistant.chat.ChatAvailability;
import com.dbn.assistant.chat.context.ChatContext;
import com.dbn.assistant.chat.window.action.AssistantActionSupport;
import com.dbn.assistant.state.AssistantState;
import com.dbn.common.action.ComboBoxAction;
import com.dbn.common.routine.Consumer;
import com.dbn.common.util.Actions;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.object.DBTable;
import com.dbn.object.action.AnObjectAction;
import com.dbn.object.action.ObjectSelectAction;
import com.dbn.object.common.ui.DBObjectSelectionInput;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.dbn.vector.DatabaseVectorManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsActions.ActionText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.dbn.assistant.chat.ChatAvailability.AVAILABLE;
import static com.dbn.nls.NlsResources.txt;

public class EmbeddingTableSelectionAction extends ComboBoxAction implements AssistantActionSupport {

    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(@NotNull JComponent component, @NotNull DataContext dataContext) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();

        AssistantState assistantState = getAssistantState(dataContext);
        if (assistantState == null) return actionGroup;

        ConnectionHandler connection = assistantState.getConnection();
        ConnectionId connectionId = connection.getConnectionId();
        Project project = connection.getProject();

        DatabaseVectorManager vectorManager = DatabaseVectorManager.getInstance(project);
        DBObjectSelectionInput<DBTable> selectorInput = vectorManager.initEmbeddingsTableSelector(connectionId);
        Set<DBObjectRef<DBTable>> embeddingTables = vectorManager.getRecentEmbeddingTables(connectionId);

        Consumer<DBTable> selectionConsumer = t -> {
            DBObjectRef<DBTable> ref = DBObjectRef.of(t);
            ChatContext chatContext = assistantState.getCurrentContext();
            chatContext.setEmbeddingTable(ref);
            embeddingTables.add(ref);
        };

        List<DBObjectRef<DBTable>> tables = new ArrayList<>(embeddingTables);
        Collections.reverse(tables);
        for (DBObjectRef<DBTable> embeddingTable : tables) {
            DBTable table = embeddingTable.get();
            if (table == null) continue;
            AnObjectAction<DBTable> selectAction = new AnObjectAction<>(table) {

                @Override
                protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project, @NotNull DBTable target) {
                    selectionConsumer.accept(target);
                }

                @Override
                protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable DBTable target) {
                    presentation.setIcon(DBObjectType.TABLE.getIcon());
                    presentation.setText(getTablePath(target.ref()));
                }
            };
            actionGroup.add(selectAction);
        }
        actionGroup.addSeparator();


        actionGroup.add(new ObjectSelectAction<>(embeddingTables.isEmpty() ?
                txt("app.assistant.action.SelectEmbeddingTable") :
                txt("app.assistant.action.MoreEmbeddingTables"), selectorInput, selectionConsumer));
        return actionGroup;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Presentation presentation = e.getPresentation();

        presentation.setText(getText(e));
        presentation.setIcon(getIcon(e));
        presentation.setEnabled(isEnabled(e));
        presentation.setVisible(isVisible(e));
        presentation.setDescription(txt("app.assistant.tooltip.EmbeddingTable"));
    }

    private @ActionText String getText(@NotNull AnActionEvent e) {
        String text = txt("app.assistant.action.EmbeddingTable");

        AssistantState assistantState = getAssistantState(e);
        if (assistantState == null) return text;

        DBObjectRef<DBTable> embeddingTable = assistantState.getEmbeddingTable();
        if (embeddingTable == null) return text;

        return Actions.adjustActionName(embeddingTable.getObjectName());
    }

    private static @NotNull String getTablePath(DBObjectRef<DBTable> table) {
        if (table == null) return txt("app.assistant.action.UndefinedEmbeddingTable");
        return Actions.adjustActionName(table.getSchemaName() + "." + table.getObjectName());
    }

    @Nullable
    private Icon getIcon(@NotNull AnActionEvent e) {
        AssistantState assistantState = getAssistantState(e);
        if (assistantState == null) return null;

        DBObjectRef<DBTable> embeddingTable = assistantState.getEmbeddingTable();
        if (embeddingTable == null) return null;

        return DBObjectType.TABLE.getIcon();
    }

    private boolean isEnabled(@NotNull AnActionEvent e) {
        ChatAvailability availability = getChatAvailability(e);
        return availability == AVAILABLE;
    }

    private boolean isVisible(@NotNull AnActionEvent e) {
        ChatContext chatContext = getCurrentChatContext(e);
        if (chatContext == null) return false;

        return chatContext.getAssistantMode() == AssistantMode.RAG;
    }
}
