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

package com.dbn.event.listener.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.event.listener.EventListenerManager;
import com.dbn.object.DBTable;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EventListenerRegistrationDialog extends DBNDialog<EventListenerRegistrationForm> {
  private final DBObjectRef<DBTable> table;
  private int mask;

  public EventListenerRegistrationDialog(Project project, DBTable table) {
    super(project, "Event Listener Registration", true);
    this.table = DBObjectRef.of(table);
    setModal(false);
    setResizable(true);
    init();
  }

  DBTable getTable() {
    return table.ensure();
  }

  @NotNull
  @Override
  protected EventListenerRegistrationForm createForm() {
    DBTable object = DBObjectRef.get(this.table);
    return new EventListenerRegistrationForm(this, object);
  }

  @Override
  public void doCancelAction() {
    super.doCancelAction();
  }

  //todo add validation layer
  /*
  at least one operation should be selected .
   */

  @Override
  protected @Nullable ValidationInfo doValidate() {
    EventListenerRegistrationForm form = getForm();
    int mask = 0;
    boolean insertOperation = form.isInsert();
    boolean updateOperation = form.isUpdate();
    boolean deleteOperation = form.isDelete();

    if (insertOperation) mask |= 2;    // INSERTOP
    if (updateOperation) mask |= 4;    // UPDATEOP
    if (deleteOperation) mask |= 8;    // DELETEOP

    this.mask = mask;
    if (!insertOperation && !updateOperation && !deleteOperation) {
      return new ValidationInfo("At least one operation should be selected !");
    }
    return super.doValidate();
  }

  @Override
  protected void doOKAction() {
    EventListenerManager.getInstance().startListening(getTable(),mask);
    super.doOKAction();
  }
}


