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
import com.dbn.common.ui.dialog.DBNDialog;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.common.util.Commons.nvl;

@Getter
public class MessageBundleDialog extends DBNDialog<MessageBundleForm> {
    @Delegate
    private final MessageBundleDialogConfig config;
    private final MessageBundle messageBundle;
    private final Action[] defaultActions = new Action[] {getCancelAction()}; // default actions

    public MessageBundleDialog(MessageBundleDialogConfig config, MessageBundle messageBundle) {
        super(config.getProject(), config.getTitle(), false);
        this.config = config;
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
    protected final Action[] createActions() {
        return nvl(config.getActions(), defaultActions);
    }
}