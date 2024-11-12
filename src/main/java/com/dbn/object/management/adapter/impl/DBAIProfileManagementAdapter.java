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

package com.dbn.object.management.adapter.impl;

import com.dbn.common.util.Unsafe;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseAssistantInterface;
import com.dbn.object.DBAIProfile;
import com.dbn.object.management.ObjectManagementAdapterFactory;
import com.dbn.object.management.ObjectManagementAdapterFactoryBase;

import java.sql.SQLException;

/**
 * Implementation of {@link ObjectManagementAdapterFactory} for objects of type {@link DBAIProfile}
 *
 * @author Dan Cioca (Oracle)
 */
public class DBAIProfileManagementAdapter extends ObjectManagementAdapterFactoryBase<DBAIProfile> {

    @Override
    protected void createObject(ConnectionHandler connection, DBNConnection conn, DBAIProfile object) throws SQLException {
        DatabaseAssistantInterface databaseInterface = connection.getAssistantInterface();
        String profileName = object.getName();
        String profileOwner = object.getSchemaName();
        String description = object.getDescription();

        String attributes = object.getAttributesJson();
        databaseInterface.createProfile(conn, profileName, attributes, description);

        // update status
        Unsafe.warned(() -> {
            if (object.isEnabled())
                databaseInterface.enableProfile(conn, profileOwner, profileName);
            else
                databaseInterface.disableProfile(conn, profileOwner, profileName);
        });
    }

    @Override
    protected void updateObject(ConnectionHandler connection, DBNConnection conn, DBAIProfile object) throws SQLException {
        DatabaseAssistantInterface databaseInterface = connection.getAssistantInterface();
        String profileName = object.getName();
        String profileOwner = object.getSchemaName();

        String attributes = object.getAttributesJson();
        databaseInterface.updateProfile(conn, profileName, attributes);

        Unsafe.warned(() -> {
            if (object.isEnabled())
                databaseInterface.enableProfile(conn, profileOwner, profileName); else
                databaseInterface.disableProfile(conn, profileOwner, profileName);
        });
    }

    @Override
    protected void deleteObject(ConnectionHandler connection, DBNConnection conn, DBAIProfile object) throws SQLException {
        DatabaseAssistantInterface databaseInterface = connection.getAssistantInterface();
        databaseInterface.deleteProfile(conn, object.getSchemaName(), object.getName());
    }

    @Override
    protected void enableObject(ConnectionHandler connection, DBNConnection conn, DBAIProfile object) throws SQLException {
        DatabaseAssistantInterface databaseInterface = connection.getAssistantInterface();
        databaseInterface.enableProfile(conn, object.getSchemaName(), object.getName());
    }

    @Override
    protected void disableObject(ConnectionHandler connection, DBNConnection conn, DBAIProfile object) throws SQLException {
        DatabaseAssistantInterface databaseInterface = connection.getAssistantInterface();
        databaseInterface.disableProfile(conn, object.getSchemaName(), object.getName());
    }
}
