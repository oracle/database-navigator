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
import com.dbn.object.factory.ObjectFactoryAdapter;
import com.dbn.object.factory.ObjectFactoryAdapters;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.sql.SQLException;

import static com.dbn.common.util.Commons.nvl;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

@Getter
public class DBObjectFactoryInputDialog extends DBNDialog<DBObjectFactoryInputForm> {
    private final DBObjectRef<DBSchema> schema;
    private final DBObjectType objectType;
    private final DBObjectSpec initialInput;

    public DBObjectFactoryInputDialog(@NotNull Project project, DBSchema schema, DBObjectType objectType, DBObjectSpec initialInput) {
        super(project, txt("msg.objects.title.CreateObject", objectType.getName()), true);
        this.schema = DBObjectRef.of(schema);
        this.objectType = objectType;
        this.initialInput = initialInput;
//        setModal(false);
        setResizable(true);
        init();
    }

    @NotNull
    @Override
    protected DBObjectFactoryInputForm createForm() {
        ObjectFactoryAdapter factoryAdapter = ObjectFactoryAdapters.get(objectType);

        DBSchema schema = getSchema();
        DBObjectSpec input = nvl(initialInput, () -> factoryAdapter.createInput(schema));
        return factoryAdapter.createInputForm(this, input);
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
    protected Action[] initializeActions() {
        renameAction(getOKAction(), "Create " + objectType.getTitleCasedName());
        return actions(
                getOKAction(),
                getCancelAction());
    }

    @Override
    public void doOKAction() {
        Project project = getProject();
        DBSchema schema = getSchema();
        DBObjectType objectType = getObjectType();

        DBObjectFactoryInputForm form = getForm();
        try {
            form.applyFormChanges();
        } catch (ConfigurationException e) {
            conditionallyLog(e);
            Messages.showErrorDialog(getProject(), e.getMessage());
            return;
        }
        DBObjectSpec input = form.getInput();
        super.doOKAction();

        String title = txt("prc.object.title.CreatingObject", input.getObjectTypeName());
        String text = txt("prc.object.text.CreatingObjectDescription", input.getObjectDescription());
        ProgressRunnable invoker = p -> invokeObjectFactory(project, schema, objectType, input);

        if (isRootDialog()) {
            // allow operation to be sent to the background
            Progress.prompt(project, schema, true, title, text, invoker);
        } else {
            Progress.modal(project, schema, true, title, text, invoker);
        }
    }

    private void invokeObjectFactory(Project project, DBSchema schema, DBObjectType objectType, DBObjectSpec input) {
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

    private static void reopenInputDialog(Project project, DBSchema schema, DBObjectType objectType, DBObjectSpec input) {
        Dialogs.show(() -> new DBObjectFactoryInputDialog(project, schema, objectType, input));
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
    }
}
