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

package com.dbn.object.filter.custom.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.object.filter.custom.ObjectFilter;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

@Getter
public class ObjectFilterDetailsDialog extends DBNDialog<ObjectFilterDetailsForm> {
    private final ObjectFilter<?> filter;

    public ObjectFilterDetailsDialog(ObjectFilter<?> filter, boolean create) {
        super(filter.getProject(), getTitle(create), true);
        this.filter = filter;

        setModal(true);
        setResizable(true);
        Action okAction = getOKAction();

        renameAction(okAction, create ? "Create" : "Update");
        init();
    }

    @NotNull
    @Override
    protected ObjectFilterDetailsForm createForm() {
        return new ObjectFilterDetailsForm(this);
    }

    private static String getTitle(boolean create) {
        return create ? "Create filter" : "Edit filter";
    }

    public void setActionEnabled(boolean enabled) {
        getOKAction().setEnabled(enabled);
    }

    @Override
    public void doOKAction() {
        ObjectFilterDetailsForm component = getForm();
        String expression = component.getExpression();
        filter.setExpression(expression);
        super.doOKAction();
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
    }
}
