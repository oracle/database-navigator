/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.data.type.ui;

import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.data.value.ComplexValue;
import com.dbn.data.value.StructureValue;
import com.dbn.object.DBType;
import com.dbn.object.DBTypeAttribute;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JComponent;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

@Getter
public class DeclaredTypeValueEditorDialog extends DBNDialog<DeclaredTypeValueEditorForm> {
    private final DBType declaredType;
    private final Object initialValue;
    private Object[] attributeValues;

    public DeclaredTypeValueEditorDialog(
            @NotNull ConnectionHandler connection,
            @NotNull DBType declaredType,
            @Nullable Object initialValue,
            @Nullable Object contextObject) {
        super(connection, txt("msg.dataEditor.title.EditRecord"), true);
        this.declaredType = declaredType;
        this.initialValue = resolveObjectValue(initialValue);
        setContextObject(contextObject);
        setModal(true);
        setResizable(true);
        setDefaultSize(640, 480);
        init();
    }

    @NotNull
    @Override
    protected DeclaredTypeValueEditorForm createForm() {
        return new DeclaredTypeValueEditorForm(this, declaredType, initialValue);
    }

    @Override
    protected Action[] initializeActions() {
        return actions(getOKAction(), getCancelAction());
    }

    public static void showEditor(
            Project project,
            JComponent dispatchTarget,
            ConnectionHandler connection,
            DBType declaredType,
            Object initialValue,
            String objectName,
            @Nullable Object contextObject,
            BooleanSupplier isCurrent,
            Consumer<Object[]> valueConsumer) {
        Progress.prompt(
                project,
                connection,
                true,
                txt("prc.dataEditor.title.OpeningRecord"),
                txt("prc.dataEditor.text.OpeningRecordFor", objectName),
                progress -> {
                    initializeTypeAttributes(declaredType, progress);
                    progress.checkCanceled();
                    Dispatch.run(dispatchTarget, () -> {
                        if (!isCurrent.getAsBoolean()) return;

                        Dialogs.show(
                                () -> new DeclaredTypeValueEditorDialog(connection, declaredType, initialValue, contextObject),
                                (dialog, exitCode) -> {
                                    if (exitCode == OK_EXIT_CODE && isCurrent.getAsBoolean()) {
                                        valueConsumer.accept(dialog.getAttributeValues());
                                    }
                                });
                    });
                });
    }

    public static void initializeTypeAttributes(DBType declaredType, ProgressIndicator progress) {
        initializeTypeAttributes(declaredType, new HashSet<>(), progress);
    }

    private static void initializeTypeAttributes(
            DBType declaredType,
            Set<DBObjectRef<DBType>> visitedTypes,
            ProgressIndicator progress) {
        progress.checkCanceled();
        if (declaredType == null || declaredType.isCollection() || !visitedTypes.add(DBObjectRef.of(declaredType))) return;

        for (DBTypeAttribute attribute : declaredType.getAttributes()) {
            initializeTypeAttributes(attribute.getDataType().getDeclaredType(), visitedTypes, progress);
        }
    }

    @Override
    protected void doOKAction() {
        try {
            attributeValues = getForm().getAttributeValues();
            super.doOKAction();
        } catch (ParseException e) {
            conditionallyLog(e);
            Messages.showErrorDialog(
                    getProject(),
                    txt("msg.dataEditor.title.FailedToOpenEditor"),
                    e.getMessage() == null ? e.toString() : e.getMessage(),
                    e);
        }
    }

    private static Object resolveObjectValue(Object value) {
        if (value instanceof StructureValue structureValue) return structureValue.getStruct();
        if (value instanceof ComplexValue complexValue) return complexValue.getObjectValue();
        return value;
    }
}
