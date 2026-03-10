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

package com.dbn.assistant.service.selectai.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

public class SelectAiHelpDialog extends DBNDialog<SelectAiHelpForm> {

  public SelectAiHelpDialog(ConnectionHandler connection) {
    super(connection, "Select AI Help", true);

    setResizable(false);
    init();
  }

  @NotNull
  @Override
  protected Action[] initializeActions() {
    renameAction(getCancelAction(), "Close");
    return actions(getCancelAction());
  }

  @Override
  protected @NotNull SelectAiHelpForm createForm() {
    return new SelectAiHelpForm(this);
  }
}
