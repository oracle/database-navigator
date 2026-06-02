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
import com.dbn.object.filter.ObjectFilterManager;
import com.dbn.object.filter.custom.ObjectFilter;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.nls.NlsResources.txt;

@Getter
public class ObjectFilterDetailsDialog extends DBNDialog<ObjectFilterDetailsForm> {
    private final ObjectFilter<?> filter;
    private final boolean create;
    private final boolean standalone;

    public ObjectFilterDetailsDialog(ObjectFilter<?> filter, boolean create, boolean standalone) {
        super(filter.getProject(), getTitle(create), true);
        this.filter = filter;
        this.create = create;
        this.standalone = standalone;

        setModal(true);
        setResizable(true);
        Action okAction = getOKAction();

        init();
    }

    @NotNull
    @Override
    protected ObjectFilterDetailsForm createForm() {
        return new ObjectFilterDetailsForm(this);
    }

    @Override
    protected Action[] initializeActions() {
        String actionName = create ? "Create" : "Update";
        renameAction(getOKAction(), actionName);

        return actions(
                getOKAction(),
                getRemoveAction(),
                getToggleAction(),
                getCancelAction());
    }

    private static String getTitle(boolean create) {
        return create ?
                txt("msg.objects.title.CreateFilter") :
                txt("msg.objects.title.EditFilter");
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

    private Action getToggleAction() {
        if (create) return null;
        if (!standalone) return null;

        String toggleName = filter.isActive() ?
                txt("app.shared.action.Disable") :
                txt("app.shared.action.Enable");
        return createAction(toggleName, () -> toggleFilter());
    }

    private Action getRemoveAction() {
        if (create) return null;
        if (!standalone) return null;

        return createAction(txt("app.shared.action.Remove"), () -> removeFilter());
    }

    private void toggleFilter() {
        ObjectFilterManager instance = ObjectFilterManager.getInstance(getProject());
        instance.toggleFilter(
                filter.getConnectionId(),
                filter.getObjectType());
        close(CLOSE_EXIT_CODE);
    }

    private void removeFilter() {
        ObjectFilterManager instance = ObjectFilterManager.getInstance(getProject());
        instance.removeFilter(
                filter.getConnectionId(),
                filter.getObjectType());
        close(CLOSE_EXIT_CODE);
    }
}
