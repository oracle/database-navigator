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

package com.dbn.object.management.adapter;

import com.dbn.object.common.DBObject;
import com.dbn.object.event.ObjectChangeAction;
import org.jetbrains.annotations.Nls;

import static com.intellij.openapi.util.NlsContexts.DialogMessage;
import static com.intellij.openapi.util.NlsContexts.DialogTitle;
import static com.intellij.openapi.util.NlsContexts.ProgressText;
import static com.intellij.openapi.util.NlsContexts.ProgressTitle;

/**
 * Abstract implementation of the {@link com.dbn.object.management.ObjectManagementAdapter} for DELETE actions,
 * providing generic process titles and messages
 *
 * @author Dan Cioca (Oracle)
 */
public final class DBObjectDeleteAdapter<T extends DBObject> extends ObjectManagementAdapterBase<T> {

    public DBObjectDeleteAdapter(T object, InterfaceInvoker<T> invoker) {
        super(object, ObjectChangeAction.DELETE, invoker);
    }

    @Nls
    @Override
    @DialogTitle
    protected String getSuccessTitle() {
        return txt("msg.objects.title.ActionSuccess_DELETE");
    }

    @Nls
    @Override
    @DialogTitle
    protected String getFailureTitle() {
        return txt("msg.objects.title.ActionFailure_DELETE");
    }

    @Nls
    @Override
    @ProgressTitle
    protected String getProcessTitle() {
        return txt("prc.object.title.DeletingObject", getObjectTypeName());
    }

    @Nls
    @Override
    @ProgressText
    protected String getProcessText() {
        return txt("prc.object.text.DeletingObject", getObjectTypeName(), getObjectName());
    }

    @Nls
    @Override
    @DialogMessage
    protected String getSuccessMessage() {
        return txt("msg.object.info.ObjectDeleteSuccess", getObjectTypeName(), getObjectName());
    }

    @Nls
    @Override
    @DialogMessage
    protected String getFailureMessage() {
        return txt("msg.object.error.ObjectDeleteFailure", getObjectType(), getObjectName());
    }
}
