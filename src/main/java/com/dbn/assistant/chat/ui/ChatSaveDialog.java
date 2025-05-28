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

import com.dbn.assistant.chat.ChatInterruptionReason;
import com.dbn.common.ui.dialog.DBNDialog;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.util.Set;

@Getter
public class ChatSaveDialog extends DBNDialog<ChatSaveForm> {
    private final ChatInterruptionReason changedField;
    private final Set<String> usedTitles;
    private String title;

    public ChatSaveDialog(Project project, ChatInterruptionReason changedField, Set<String> usedTitles) {
        super(project, "Save Chat", true);
        this.changedField = changedField;
        this.usedTitles = usedTitles;
        renameAction(getOKAction(), "Save");
        setModal(true);
        init();
    }

    @NotNull
    @Override
    protected ChatSaveForm createForm() {
        return new ChatSaveForm(this, changedField, usedTitles);
    }


    @Override
    @NotNull
    protected final Action @NotNull [] createActions() {
        return new Action[]{
                getCancelAction(),
                discardAction,
                getOKAction()
        };
    }

    @Override
    public void doCancelAction() {
        close(0);
    }
    private final Action discardAction = new AbstractAction("Discard") {
        @Override
        public void actionPerformed(ActionEvent e) {
            close(1);
        }
    };
    @Override
    protected void doOKAction() {
        this.title = getForm().getChatName();
        close(2);
    }


    @Override
    @NotNull
    public Action getOKAction() {
        return super.getOKAction();
    }
}

