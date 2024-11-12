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

import com.dbn.common.exception.Exceptions;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.event.ObjectChangeAction;
import com.dbn.object.management.adapter.DBObjectCreateAdapter;
import com.dbn.object.management.adapter.DBObjectDeleteAdapter;
import com.dbn.object.management.adapter.DBObjectDisableAdapter;
import com.dbn.object.management.adapter.DBObjectEnableAdapter;
import com.dbn.object.management.adapter.DBObjectUpdateAdapter;

import java.sql.SQLException;

public abstract class ObjectManagementAdapterFactoryBase<T extends DBSchemaObject> implements ObjectManagementAdapterFactory<T> {
    @Override
    public final ObjectManagementAdapter<T> createAdapter(T object, ObjectChangeAction action) {
        switch (action) {
            case CREATE: return new DBObjectCreateAdapter<>(object, (d, c, o) -> createObject(d, c, o));
            case UPDATE: return new DBObjectUpdateAdapter<>(object, (d, c, o) -> updateObject(d, c, o));
            case DELETE: return new DBObjectDeleteAdapter<>(object, (d, c, o) -> deleteObject(d, c, o));
            case ENABLE: return new DBObjectEnableAdapter<>(object, (d, c, o) -> enableObject(d, c, o));
            case DISABLE: return new DBObjectDisableAdapter<>(object, (d, c, o) -> disableObject(d, c, o));
            default: return Exceptions.unsupported(action);
        }
    }

    protected abstract void createObject(ConnectionHandler connection, DBNConnection conn, T object) throws SQLException;
    protected abstract void updateObject(ConnectionHandler connection, DBNConnection conn, T object) throws SQLException;
    protected abstract void deleteObject(ConnectionHandler connection, DBNConnection conn, T object) throws SQLException;
    protected abstract void enableObject(ConnectionHandler connection, DBNConnection conn, T object) throws SQLException;
    protected abstract void disableObject(ConnectionHandler connection, DBNConnection conn, T object) throws SQLException;
}
