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

package com.dbn.execution.java.browser.action;

import com.dbn.common.ref.WeakRef;
import com.dbn.execution.java.browser.ui.JavaExecutionBrowserForm;
import com.dbn.object.DBSchema;
import com.dbn.object.action.AnObjectAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class SchemaSelectAction extends AnObjectAction<DBSchema> {
    private WeakRef<JavaExecutionBrowserForm> browserComponent;

    SchemaSelectAction(JavaExecutionBrowserForm browserComponent, DBSchema schema) {
        super(schema);
        this.browserComponent = WeakRef.of(browserComponent);
    }

    @Override
    protected void actionPerformed(
            @NotNull AnActionEvent e,
            @NotNull Project project,
            @NotNull DBSchema object) {

        JavaExecutionBrowserForm browserForm = browserComponent.get();
        if (browserForm != null) {
            browserForm.setSchema(object);
        }
    }


}
