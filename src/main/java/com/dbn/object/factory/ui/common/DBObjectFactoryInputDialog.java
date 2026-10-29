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
import com.dbn.common.ref.WeakRef;
import com.dbn.common.thread.Progress;
import com.dbn.common.thread.ProgressRunnable;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Conditional;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.connection.DatabaseEntity;
import com.dbn.diagnostics.Diagnostics;
import com.dbn.object.factory.DatabaseObjectFactory;
import com.dbn.object.factory.ObjectFactoryAdapter;
import com.dbn.object.factory.ObjectFactoryAdapters;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.sql.SQLException;

import static com.dbn.common.exception.Exceptions.getLocalizedMessage;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

@Getter
public class DBObjectFactoryInputDialog extends DBNDialog<DBObjectFactoryInputForm> {
    private final WeakRef<DatabaseEntity> parentEntity;
    private final DBObjectType objectType;
    private final DBObjectSpec initialInput;

    public DBObjectFactoryInputDialog(@NotNull Project project, DatabaseEntity parentEntity, DBObjectType objectType, DBObjectSpec initialInput) {
        super(project, txt("msg.objects.title.CreateObject", objectType.getDisplayName()), true);
        this.parentEntity = WeakRef.of(parentEntity);
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

        DatabaseEntity parentEntity = getParentEntity();
        DBObjectSpec input = nvl(initialInput, () -> factoryAdapter.createInput(parentEntity));
        return factoryAdapter.createInputForm(this, input);
    }

    private DatabaseEntity getParentEntity() {
        return this.parentEntity.ensure();
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
        renameAction(getOKAction(), txt("msg.objects.button.CreateObject", objectType.getTitleCasedDisplayName()));
        return actions(
                getOKAction(),
                getCancelAction());
    }

    @Override
    public void doOKAction() {
        Project project = getProject();
        DatabaseEntity parentEntity = getParentEntity();
        DBObjectType objectType = getObjectType();

        DBObjectFactoryInputForm form = getForm();
        try {
            form.applyFormChanges();
        } catch (ConfigurationException e) {
            conditionallyLog(e);
            Messages.showErrorDialog(getProject(), getLocalizedMessage(e));
            return;
        }
        DBObjectSpec input = form.getInput();
        super.doOKAction();

        String title = txt("prc.object.title.CreatingObject", input.getObjectTypeName());
        String text = txt("prc.object.text.CreatingObjectDescription", input.getObjectDescription());
        ProgressRunnable invoker = p -> invokeObjectFactory(project, parentEntity, objectType, input);

        if (isRootDialog()) {
            // allow operation to be sent to the background
            Progress.prompt(project, parentEntity, true, title, text, invoker);
        } else {
            Progress.modal(project, parentEntity, true, title, text, invoker);
        }
    }

    private void invokeObjectFactory(Project project, DatabaseEntity parentEntity, DBObjectType objectType, DBObjectSpec input) {
        DatabaseObjectFactory factory = DatabaseObjectFactory.getInstance(project);
        try {
            factory.createObject(input);
        } catch (SQLException e) {
            //Messages.showErrorDialog(project, "Failed to create " + input.getObjectTypeName() + ".", e);

            InteractiveMessage message =
                    InteractiveMessage.error(
                            txt("msg.objects.title.ObjectCreationFailed"),
                            txt("msg.objects.error.ObjectCreationFailed", input.getObjectTypeName())).
                    withException(e).
                    withOptions(Messages.OPTIONS_RETRY_CANCEL, 0).
                    withCallback(o -> Conditional.when(o == 0, () -> reopenInputDialog(project, parentEntity, objectType, input)));
            Messages.showMessageDialog(project, message);
        }

    }

    private static void reopenInputDialog(Project project, DatabaseEntity parentEntity, DBObjectType objectType, DBObjectSpec input) {
        Dialogs.show(() -> new DBObjectFactoryInputDialog(project, parentEntity, objectType, input));
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
    }
}
