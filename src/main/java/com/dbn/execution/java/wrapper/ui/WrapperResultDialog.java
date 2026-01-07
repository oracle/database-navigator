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

package com.dbn.execution.java.wrapper.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.execution.java.wrapper.WrapperModel;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

/**
 * Wrapper factory result dialog
 * Lists all the database objects that were created as part of execution wrapper factory activity
 *
 * @author Dan Cioca (Oracle)
 */
public class WrapperResultDialog extends DBNDialog<WrapperResultForm> {

  private final WrapperModel model;

  public WrapperResultDialog(Project project, WrapperModel model) {
    super(project, "Wrapper Result", false);
    //this.setDefaultSize(380, 420);
    this.setModal(true);
    this.setAutoSize(true);
    this.model = model;
    init();
  }

  @NotNull
  @Override
  protected Action[] initializeActions() {
    renameAction(getCancelAction(), "Close");
    return actions(getCancelAction());
  }

  @Override
  protected @NotNull WrapperResultForm createForm() {
    return new WrapperResultForm(this, model);
  }
}
