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

package com.dbn.assistant.service;

import com.dbn.common.Priority;
import com.dbn.common.component.ConnectionComponent;
import com.dbn.common.routine.ParametricCallable;
import com.dbn.common.routine.ParametricRunnable;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseAssistantInterface;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

/**
 * Component stub for database assistant services
 * Exposes utilities from {@link ConnectionComponent} as well as connectivity and interface access
 *
 * @author Dan Cioca (Oracle)
 */
public abstract class AIServiceBase extends ConnectionComponent {
    public AIServiceBase(@NotNull ConnectionHandler connection) {
        super(connection);
    }

    protected final DatabaseAssistantInterface getAssistantInterface() {
        return getConnection().getAssistantInterface();
    }

    /**
     * Executes a database interface call using the controlled {@link DatabaseInterfaceInvoker} managed threads
     * @param callable the task to execute inside database interface managed threads
     * @param <T> the return type expected from the interface
     * @throws SQLException if task execution fails
     */
    protected <T> T executeCall(ParametricCallable<DBNConnection, T, SQLException> callable) throws SQLException {
        return DatabaseInterfaceInvoker.load(Priority.HIGH, getProject(), getConnectionId(), conn -> callable.call(conn));
    }

    /**
     * Executes a database interface task using the controlled {@link DatabaseInterfaceInvoker} managed threads
     * @param runnable the task to execute inside database interface managed threads
     * @throws SQLException if task execution fails
     */
    protected void executeTask(ParametricRunnable<DBNConnection, SQLException> runnable) throws SQLException {
        DatabaseInterfaceInvoker.execute(Priority.HIGH, getProject(), getConnectionId(), conn -> runnable.run(conn));
    }
}
