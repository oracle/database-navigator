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

package com.dbn.object.factory.ui.common;

import com.dbn.common.dispose.Failsafe;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Messages;
import com.dbn.diagnostics.Diagnostics;
import com.dbn.object.DBSchema;
import com.dbn.object.factory.DatabaseObjectFactory;
import com.dbn.object.factory.ObjectFactoryInput;
import com.dbn.object.factory.ui.FunctionFactoryInputForm;
import com.dbn.object.factory.ui.JavaFactoryInputForm;
import com.dbn.object.factory.ui.ProcedureFactoryInputForm;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class ObjectFactoryInputDialog extends DBNDialog<ObjectFactoryInputForm<?>> {
    private final DBObjectRef<DBSchema> schema;
    private final DBObjectType objectType;

    public ObjectFactoryInputDialog(@NotNull Project project, DBSchema schema, DBObjectType objectType) {
        super(project, "Create " + objectType.getName(), true);
        this.schema = DBObjectRef.of(schema);
        this.objectType = objectType;
        setModal(false);
        setResizable(true);
        init();
    }

    @NotNull
    @Override
    protected ObjectFactoryInputForm<?> createForm() {
        DBSchema schema = getSchema();
        return objectType == DBObjectType.FUNCTION ? new FunctionFactoryInputForm(this, schema, objectType, 0) :
               objectType == DBObjectType.PROCEDURE ? new ProcedureFactoryInputForm(this, schema, objectType, 0) :
               objectType == DBObjectType.JAVA_CLASS ? new JavaFactoryInputForm(this,schema, 0):
                       Failsafe.nn(null);
    }

    private DBSchema getSchema() {
        return this.schema.ensure();
    }

    @Override
    protected String getDimensionServiceKey() {
        // use custom dimension service keys for every object type
        return Diagnostics.isDialogSizingReset() ? null : super.getDimensionServiceKey() + "." + objectType;
    }

    @Override
    public void doOKAction() {
        ObjectFactoryInputForm form = getForm();
        ObjectFactoryInput input = form.createFactoryInput(null);
        Progress.modal(
                getProject(),
                getSchema(), true,
                "Creating " + input.getObjectTypeName(),
                "Creating " + input.getObjectDescription(),
                p -> invokeObjectFactory(p, input));
    }

    private void invokeObjectFactory(ProgressIndicator progress, ObjectFactoryInput input) {
        Project project = getProject();

        DatabaseObjectFactory factory = DatabaseObjectFactory.getInstance(project);
        try {
            factory.createObject(input);
            if (progress.isCanceled()) return; // do not close the dialog if cancelled

            Dispatch.run(getComponent(), () -> super.doOKAction());
        } catch (SQLException e) {
            Messages.showErrorDialog(project, "Failed to create " + input.getObjectTypeName() + ".", e);
        }
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
    }
}
