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

package com.dbn.object.common;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Common purpose wrapper for entities of type {@link DBObject}.
 * It holds a weak reference to the object and exposes context utilities like project, connection, and owner information
 * @param <T> the type of the wrapped object
 *
 * @author Dan Cioca (Oracle)
 */
public class DBObjectWrapper<T extends DBObject> {
    private final DBObjectRef<T> object;

    public DBObjectWrapper(@NotNull T object) {
        this.object = DBObjectRef.of(object);
    }

    @NotNull
    protected T getObject() {
        return object.ensure();
    }

    @NotNull
    protected ConnectionHandler getConnection() {
        return getObject().getConnection();
    }

    protected ConnectionId getConnectionId() {
        return getObject().getConnectionId();
    }

    protected SchemaId getOwnerId() {
        return getObject().getSchemaId();
    }

    @NotNull
    protected Project getProject() {
        return getObject().getProject();
    }

}
