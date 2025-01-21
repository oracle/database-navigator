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

package com.dbn.object.filter.quick.action;

import com.dbn.common.action.BasicAction;
import com.dbn.common.icon.Icons;
import com.dbn.common.ref.WeakRef;
import com.dbn.object.filter.quick.ui.ObjectQuickFilterConditionForm;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

public class EnableDisableQuickFilterConditionAction extends BasicAction {
    private final WeakRef<ObjectQuickFilterConditionForm> conditionForm;

    public EnableDisableQuickFilterConditionAction(ObjectQuickFilterConditionForm conditionForm) {
        this.conditionForm = WeakRef.of(conditionForm);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        ObjectQuickFilterConditionForm conditionForm = getConditionForm();

        Presentation presentation = e.getPresentation();
        presentation.setIcon(
                conditionForm.isActive() ?
                        Icons.COMMON_FILTER_ACTIVE :
                        Icons.COMMON_FILTER_INACTIVE);
        presentation.setText(
                conditionForm.isActive() ?
                        txt("app.objects.action.DeactivateCondition") :
                        txt("app.objects.action.ActivateCondition"));
    }

    @Override

    public void actionPerformed(@NotNull AnActionEvent e) {
        ObjectQuickFilterConditionForm conditionForm = getConditionForm();
        conditionForm.setActive(!conditionForm.isActive());
    }

    private ObjectQuickFilterConditionForm getConditionForm() {
        return WeakRef.ensure(conditionForm);
    }

}

