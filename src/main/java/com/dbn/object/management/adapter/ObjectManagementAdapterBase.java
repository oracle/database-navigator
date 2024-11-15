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

import com.dbn.common.notification.NotificationGroup;
import com.dbn.common.outcome.Outcome;
import com.dbn.common.outcome.OutcomeHandler;
import com.dbn.common.outcome.OutcomeHandlers;
import com.dbn.common.outcome.OutcomeHandlersImpl;
import com.dbn.common.outcome.OutcomeType;
import com.dbn.common.outcome.Outcomes;
import com.dbn.common.thread.InvocationType;
import com.dbn.common.thread.Progress;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.diagnostics.Diagnostics;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectWrapper;
import com.dbn.object.event.ObjectChangeAction;
import com.dbn.object.event.ObjectChangeNotifier;
import com.dbn.object.management.ObjectManagementAdapter;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nls;

import java.sql.SQLException;

import static com.dbn.common.Priority.HIGHEST;
import static com.dbn.common.exception.Exceptions.unsupported;

/**
 * Abstract base implementation of an {@link ObjectManagementAdapter}
 * Forces the actual adapter implementers to provide the logic for the actual {@link com.dbn.database.interfaces.DatabaseInterface} interaction,
 * as well as the various titles and captions to be displayed in the progress elements and outcome confirmation messages
 * @param <T> the type of the database object being handled by the adapter
 *
 * @author Dan Cioca (Oracle)
 */
@Getter
@Setter
abstract class ObjectManagementAdapterBase<T extends DBObject> extends DBObjectWrapper<T> implements ObjectManagementAdapter<T> {
    private final ObjectChangeAction action;
    private final DBObjectType objectType;
    private final OutcomeHandlers outcomeHandlers = new OutcomeHandlersImpl();
    private final InterfaceInvoker<T> invoker;

    ObjectManagementAdapterBase(T object, ObjectChangeAction action, InterfaceInvoker<T> invoker) {
        super(object);
        this.objectType = object.getObjectType();
        this.action = action;
        this.invoker = invoker;

        outcomeHandlers.addHandler(OutcomeType.SUCCESS, ObjectChangeNotifier.create(getConnection(), getOwnerId(), objectType, action));
        outcomeHandlers.addNotificationHandler(OutcomeType.SUCCESS, getProject(), NotificationGroup.DDL);
        outcomeHandlers.addMessageHandler(OutcomeType.FAILURE, getProject());
    }

    @Override
    public final void addOutcomeHandler(OutcomeType outcomeType, OutcomeHandler handler) {
        if (handler == null) return;
        outcomeHandlers.addHandler(outcomeType, handler);
    }

    @Override
    public final void invokeModal() {
        T object = getObject();
        Progress.modal(getProject(), getConnection(), true,
                getProcessTitle(),
                getProcessDescription(),
                progress -> invoke());
    }

    @Override
    public void invokePrompted() {
        T object = getObject();
        Progress.prompt(getProject(), getConnection(), true,
                getProcessTitle(),
                getProcessDescription(),
                progress -> invoke());
    }

    @Override
    public final void invokeInBackground() {
        T object = getObject();
        Progress.background(getProject(), getConnection(), true,
                getProcessTitle(),
                getProcessDescription(),
                progress -> invoke());
    }
    
    public final void invoke(InvocationType invocationType) {
        switch (invocationType) {
            case MODAL: invokeModal(); break;
            case PROMPTED: invokePrompted(); break;
            case BACKGROUND: invokeInBackground(); break;
            default: unsupported(invocationType);
        }
    }

    public final void invoke() {
        T object = getObject();
        try {
            DatabaseInterfaceInvoker.execute(HIGHEST,
                    getProcessTitle(),
                    getProcessDescription(),
                    getProject(),
                    getConnectionId(),
                    getOwnerId(),
                    conn -> invoker.invokeDatabaseInterface(getConnection(), conn, object));

            handleSuccess(object);
        } catch (Exception e) {
            Diagnostics.conditionallyLog(e);
            handleFailure(object, e);
        }
    }

    protected void handleSuccess(T object) {
        Outcome outcome = Outcomes.success(getSuccessTitle(), getSuccessMessage());
        outcomeHandlers.handle(outcome);
    }

    protected void handleFailure(T object, Exception e) {
        Outcome outcome = Outcomes.failure(getFailureTitle(), getFailureMessage(), e);
        outcomeHandlers.handle(outcome);
    }

    protected String getObjectTypeName() {
        return getObjectType().getName();
    }

    protected String getObjectName() {
        return getObject().getQualifiedName();
    }

    @Nls
    protected abstract String getProcessDescription();

    @Nls
    protected abstract String getSuccessMessage();

    @Nls
    protected abstract String getFailureMessage();

    @Nls
    protected abstract String getProcessTitle();

    @Nls
    protected abstract String getSuccessTitle();

    @Nls
    protected abstract String getFailureTitle();

    @FunctionalInterface
    public interface InterfaceInvoker<T extends DBObject> {
        void invokeDatabaseInterface(ConnectionHandler connection, DBNConnection conn, T object) throws SQLException;
    }

}
