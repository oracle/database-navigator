/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * This software is dual-licensed to you under the Universal Permissive License
 * (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl or Apache License
 * 2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
 * either license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.dbn.object.management;

import com.dbn.common.notification.NotificationGroup;
import com.dbn.common.outcome.*;
import com.dbn.common.thread.Progress;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.diagnostics.Diagnostics;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBObjectWrapper;
import com.dbn.object.event.ObjectChangeAction;
import com.dbn.object.event.ObjectChangeNotifier;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static com.dbn.common.Priority.HIGHEST;

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
public abstract class ObjectManagementAdapterBase<T extends DBObject> extends DBObjectWrapper<T> implements ObjectManagementAdapter<T> {
    private final ObjectChangeAction action;
    private final DBObjectType objectType;
    private final OutcomeHandlers outcomeHandlers = new OutcomeHandlersImpl();

    public ObjectManagementAdapterBase(@NotNull T object, ObjectChangeAction action) {
        super(object);
        this.objectType = object.getObjectType();
        this.action = action;

        outcomeHandlers.addHandler(OutcomeType.SUCCESS, ObjectChangeNotifier.create(getConnection(), getOwnerId(), objectType, action));
        outcomeHandlers.addNotificationHandler(OutcomeType.SUCCESS, getProject(), NotificationGroup.ASSISTANT);
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
                getProcessDescription(object),
                progress -> invoke());
    }

    @Override
    public void invokePrompted() {
        T object = getObject();
        Progress.prompt(getProject(), getConnection(), true,
                getProcessTitle(),
                getProcessDescription(object),
                progress -> invoke());
    }

    @Override
    public final void invokeInBackground() {
        T object = getObject();
        Progress.background(getProject(), getConnection(), true,
                getProcessTitle(),
                getProcessDescription(object),
                progress -> invoke());
    }

    public final void invoke() {
        T object = getObject();
        try {
            DatabaseInterfaceInvoker.execute(HIGHEST,
                    getProcessTitle(),
                    getProcessDescription(object),
                    getProject(),
                    getConnectionId(),
                    getOwnerId(),
                    conn -> invokeDatabaseInterface(getConnection(), conn, object));

            handleSuccess(object);
        } catch (Exception e) {
            Diagnostics.conditionallyLog(e);
            handleFailure(object, e);
        }
    }

    protected void handleSuccess(T object) {
        Outcome outcome = Outcomes.success(getSuccessTitle(), getSuccessMessage(object));
        outcomeHandlers.handle(outcome);
    }

    protected void handleFailure(T object, Exception e) {
        Outcome outcome = Outcomes.failure(getFailureTitle(), getFailureMessage(object), e);
        outcomeHandlers.handle(outcome);
    }

    protected abstract void invokeDatabaseInterface(ConnectionHandler connection, DBNConnection conn, T object) throws SQLException;

    @Nls
    protected abstract String getProcessDescription(T object);

    @Nls
    protected abstract String getSuccessMessage(T object);

    @Nls
    protected abstract String getFailureMessage(T object);

    @Nls
    protected abstract String getProcessTitle();

    @Nls
    protected String getSuccessTitle() {
        // refrain from using key composition (would make key refactoring cumbersome)
        switch (action) {
            case CREATE: return txt("msg.objects.title.ActionSuccess_CREATE");
            case UPDATE: return txt("msg.objects.title.ActionSuccess_UPDATE");
            case DELETE: return txt("msg.objects.title.ActionSuccess_DELETE");
            case ENABLE: return txt("msg.objects.title.ActionSuccess_ENABLE");
            case DISABLE: return txt("msg.objects.title.ActionSuccess_DISABLE");
            default: return txt("msg.objects.title.ActionSuccess");
        }
    }

    @Nls
    protected  String getFailureTitle() {
        // refrain from using key composition (would make key refactoring cumbersome)
        switch (action) {
            case CREATE: return txt("msg.objects.title.ActionFailure_CREATE");
            case UPDATE: return txt("msg.objects.title.ActionFailure_UPDATE");
            case DELETE: return txt("msg.objects.title.ActionFailure_DELETE");
            case ENABLE: return txt("msg.objects.title.ActionFailure_ENABLE");
            case DISABLE: return txt("msg.objects.title.ActionFailure_DISABLE");
            default: return txt("msg.objects.title.ActionFailure");
        }
    }

}
