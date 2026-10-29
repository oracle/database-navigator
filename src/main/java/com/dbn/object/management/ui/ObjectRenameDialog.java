/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.object.management.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.nls.NlsResources.txt;

public class ObjectRenameDialog extends DBNDialog<ObjectRenameForm> {
    private final DBObjectRef<DBObject> object;
    private String newName;

    public ObjectRenameDialog(DBObject object) {
        super(object.getProject(), txt("msg.objects.title.RenameObject"), true);
        this.object = DBObjectRef.of(object);
        setModal(true);
        setResizable(false);
        init();
    }

    @Override
    protected @NotNull ObjectRenameForm createForm() {
        return new ObjectRenameForm(this, object.ensure());
    }

    @Override
    protected @NotNull Action[] initializeActions() {
        renameAction(getOKAction(), txt("msg.shared.button.Rename"));
        return actions(getOKAction(), getCancelAction());
    }

    @Override
    protected void doOKAction() {
        newName = getForm().getNewName();
        super.doOKAction();
    }

    public String getNewName() {
        return newName;
    }
}
