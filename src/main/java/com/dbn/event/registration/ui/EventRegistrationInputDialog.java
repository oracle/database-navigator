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

package com.dbn.event.registration.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.event.registration.EventRegistrationManager;
import com.dbn.help.HelpTopic;
import com.dbn.object.DBTable;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;

import static com.dbn.nls.NlsResources.txt;

public class EventRegistrationInputDialog extends DBNDialog<EventRegistrationInputForm> {
  private final DBObjectRef<DBTable> table;
  private int mask;

  public EventRegistrationInputDialog(Project project, DBTable table) {
    super(project, txt("msg.events.title.EventListenerRegistration"), true);
    this.table = DBObjectRef.of(table);
    setModal(false);
    setResizable(true);
    setDefaultSize(500, 400);
    init();
  }

  DBTable getTable() {
    return table.ensure();
  }

  @NotNull
  @Override
  protected EventRegistrationInputForm createForm() {
    DBTable object = DBObjectRef.get(this.table);
    return new EventRegistrationInputForm(this, object);
  }

  @Override
  protected HelpTopic getHelpTopic() {
    return HelpTopic.EVENTS_REGISTRATION;
  }

  @Override
  protected Action[] initializeActions() {
    return actions(
            getOKAction(),
            getCancelAction());
  }

//todo add validation layer
  /*
  at least one operation should be selected .
   */

  @Override
  protected @Nullable ValidationInfo doValidate() {
    EventRegistrationInputForm form = getForm();
    int mask = 0;
    boolean insertOperation = form.isInsert();
    boolean updateOperation = form.isUpdate();
    boolean deleteOperation = form.isDelete();

    if (insertOperation) mask |= 2;    // INSERTOP
    if (updateOperation) mask |= 4;    // UPDATEOP
    if (deleteOperation) mask |= 8;    // DELETEOP

    this.mask = mask;
    if (!insertOperation && !updateOperation && !deleteOperation) {
      return new ValidationInfo(txt("msg.events.error.OperationRequired"));
    }
    return super.doValidate();
  }

  @Override
  protected void doOKAction() {
    Project project = getProject();
    EventRegistrationManager registrationManager = EventRegistrationManager.getInstance(project);
    registrationManager.startListening(getTable(), mask);
    super.doOKAction();
  }
}
