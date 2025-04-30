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

package com.dbn.common.message.ui;

import com.dbn.common.message.MessageBundle;
import com.dbn.common.message.TitledMessageBundle;
import com.dbn.common.ui.dialog.DBNDialog;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

@Getter
public class MessageBundleDialog extends DBNDialog<MessageBundleForm> {
    private final MessageBundle messageBundle;

    public MessageBundleDialog(Project project, TitledMessageBundle messageBundle) {
        super(project, messageBundle.getTitle(), false);
        this.messageBundle = messageBundle;

        renameAction(getCancelAction(), "Close");
        setAutoSize(true);
        init();
    }

    @NotNull
    @Override
    protected MessageBundleForm createForm() {
        return new MessageBundleForm(this);
    }


    @Override
    protected final Action @NotNull [] createActions() {
        return new Action[]{
                getCancelAction()};
    }
}