package com.dbn.object.factory.ui.common;

import com.dbn.common.dispose.Failsafe;
import com.dbn.common.thread.Callback;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Messages;
import com.dbn.object.DBSchema;
import com.dbn.object.factory.DatabaseObjectFactory;
import com.dbn.object.factory.ObjectFactoryInput;
import com.dbn.object.factory.ui.FunctionFactoryInputForm;
import com.dbn.object.factory.ui.ProcedureFactoryInputForm;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

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
        DBSchema schema = this.schema.ensure();
        return objectType == DBObjectType.FUNCTION ? new FunctionFactoryInputForm(this, schema, objectType, 0) :
               objectType == DBObjectType.PROCEDURE ? new ProcedureFactoryInputForm(this, schema, objectType, 0) :
                       Failsafe.nn(null);
    }

    @Override
    public void doOKAction() {
        Project project = getProject();

        ObjectFactoryInputForm form = getForm();
        ObjectFactoryInput factoryInput = form.createFactoryInput(null);
        String objectTypeName = factoryInput.getObjectType().getName();

        Callback callback = Callback.create();
        callback.before(() -> form.freezeForm());
        callback.onSuccess(() -> Dispatch.run(() -> super.doOKAction()));
        callback.onFailure(e -> Messages.showErrorDialog(project, "Could not create " + objectTypeName + ".", e));
        callback.after(() -> form.unfreezeForm());

        DatabaseObjectFactory factory = DatabaseObjectFactory.getInstance(project);
        factory.createObject(factoryInput, callback);
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
    }
}
