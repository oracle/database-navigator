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

package com.dbn.object.common.ui;

import com.dbn.common.routine.Consumer;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.object.common.DBObject;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

@Getter
public class DBObjectSelectionDialog<T extends DBObject> extends DBNDialog<DBObjectSelectionForm<T>> {
    private final DBObjectSelectionInput<T> input;
    private final Consumer<T> callback;

    public DBObjectSelectionDialog(DBObjectSelectionInput<T> input, Consumer<T> callback) {
        super(input.getConnection(), "Select " + input.getObjectType().getTitleCasedName(), false);
        this.input = input;
        this.callback = callback;
        init();
    }

    @Override
    protected @NotNull DBObjectSelectionForm<T> createForm() {
        return new DBObjectSelectionForm<T>(this, input);
    }

    @Override
    protected Action[] initializeActions() {
        renameAction(getOKAction(), "Select");
        return actions(
                getOKAction(),
                getCancelAction());
    }

    @Override
    @SneakyThrows
    protected void doOKAction() {
        T selectedObject = getForm().getSelectedObject();
        callback.accept(selectedObject);
        super.doOKAction();
    }

    @Override
    @SneakyThrows
    public void doCancelAction() {
        super.doCancelAction();
    }
}
