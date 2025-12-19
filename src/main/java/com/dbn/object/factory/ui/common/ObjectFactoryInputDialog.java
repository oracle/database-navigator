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
import com.dbn.common.message.InteractiveMessage;
import com.dbn.common.thread.Progress;
import com.dbn.common.thread.ProgressRunnable;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Conditional;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.diagnostics.Diagnostics;
import com.dbn.object.DBSchema;
import com.dbn.object.factory.DatabaseObjectFactory;
import com.dbn.object.factory.ObjectFactoryInput;
import com.dbn.object.factory.ui.JavaFactoryInputForm;
import com.dbn.object.factory.ui.MethodFactoryInputForm;
import com.dbn.object.factory.ui.ModelFactoryInputForm;
import com.dbn.object.factory.ui.TableFactoryInputForm;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Getter
public class ObjectFactoryInputDialog extends DBNDialog<ObjectFactoryInputForm<?>> {
    private final DBObjectRef<DBSchema> schema;
    private final DBObjectType objectType;
    private final ObjectFactoryInput initialInput;

    public ObjectFactoryInputDialog(@NotNull Project project, DBSchema schema, DBObjectType objectType) {
        this(project, schema, objectType, null);
    }

    public ObjectFactoryInputDialog(@NotNull Project project, DBSchema schema, DBObjectType objectType, ObjectFactoryInput initialInput) {
        super(project, "Create " + objectType.getName(), true);
        this.schema = DBObjectRef.of(schema);
        this.objectType = objectType;
        this.initialInput = initialInput;
//        setModal(false);
        setResizable(true);
        init();
    }

    @NotNull
    @Override
    protected ObjectFactoryInputForm<?> createForm() {
        DBSchema schema = getSchema();
        ObjectFactoryInputForm inputForm =
                objectType == DBObjectType.TABLE ? new TableFactoryInputForm(this, schema) :
                objectType == DBObjectType.FUNCTION ? new MethodFactoryInputForm(this, schema, DBObjectType.FUNCTION) :
                objectType == DBObjectType.PROCEDURE ? new MethodFactoryInputForm(this, schema, DBObjectType.PROCEDURE) :
                objectType == DBObjectType.JAVA_CLASS ? new JavaFactoryInputForm(this, schema) :
                objectType == DBObjectType.AI_MODEL ? new ModelFactoryInputForm(this, schema):
                        Failsafe.nn(null);

        if (initialInput != null) {
            inputForm.restoreUserInput(initialInput);
        }
        return inputForm;
    }

    private DBSchema getSchema() {
        return this.schema.ensure();
    }

    public String getObjectName() {
        return getForm().getObjectName();
    }

    @Override
    protected String getDimensionServiceKey() {
        // use custom dimension service keys for every object type
        return Diagnostics.isDialogSizingReset() ? null : super.getDimensionServiceKey() + "." + objectType;
    }

    @Override
    public void doOKAction() {
        Project project = getProject();
        DBSchema schema = getSchema();
        DBObjectType objectType = getObjectType();

        ObjectFactoryInputForm form = getForm();
        try {
            form.applyFormChanges();
        } catch (ConfigurationException e) {
            conditionallyLog(e);
            Messages.showErrorDialog(getProject(), e.getMessage());
            return;
        }
        ObjectFactoryInput input = form.getFactoryInput();
        super.doOKAction();

        String title = "Creating " + input.getObjectTypeName();
        String text = "Creating " + input.getObjectDescription();
        ProgressRunnable invoker = p -> invokeObjectFactory(project, schema, objectType, input);

        if (isRootDialog()) {
            // allow operation to be sent to the background
            Progress.prompt(project, schema, true, title, text, invoker);
        } else {
            Progress.modal(project, schema, true, title, text, invoker);
        }
    }

    private void invokeObjectFactory(Project project, DBSchema schema, DBObjectType objectType, ObjectFactoryInput input) {
        DatabaseObjectFactory factory = DatabaseObjectFactory.getInstance(project);
        try {
            factory.createObject(input);
        } catch (SQLException e) {
            //Messages.showErrorDialog(project, "Failed to create " + input.getObjectTypeName() + ".", e);

            InteractiveMessage message =
                    InteractiveMessage.error("Object creation failed", "Failed to create " + input.getObjectTypeName() + ".").
                    withException(e).
                    withOptions(Messages.OPTIONS_RETRY_CANCEL, 0).
                    withCallback(o -> Conditional.when(o == 0, () -> reopenInputDialog(project, schema, objectType, input)));
            Messages.showMessageDialog(project, message);
        }

    }

    private static void reopenInputDialog(Project project, DBSchema schema, DBObjectType objectType, ObjectFactoryInput input) {
        Dialogs.show(() -> new ObjectFactoryInputDialog(project, schema, objectType, input));
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
    }
}
