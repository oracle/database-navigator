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

package com.dbn.object.management.adapter.impl;

import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseAssistantInterface;
import com.dbn.object.DBCredential;
import com.dbn.object.management.ObjectManagementAdapterFactory;
import com.dbn.object.management.ObjectManagementAdapterFactoryBase;
import com.dbn.object.type.DBAttributeType;
import com.dbn.object.type.DBCredentialType;

import java.sql.SQLException;
import java.util.Map;

import static com.dbn.object.type.DBAttributeType.FINGERPRINT;
import static com.dbn.object.type.DBAttributeType.PASSWORD;
import static com.dbn.object.type.DBAttributeType.PRIVATE_KEY;
import static com.dbn.object.type.DBAttributeType.USER_NAME;
import static com.dbn.object.type.DBAttributeType.USER_OCID;
import static com.dbn.object.type.DBAttributeType.USER_TENANCY_OCID;

/**
 * Implementation of {@link ObjectManagementAdapterFactory} for objects of type {@link DBCredential}
 * @author Dan Cioca (Oracle)
 */
public class DBCredentialManagementAdapter extends ObjectManagementAdapterFactoryBase<DBCredential> {

    @Override
    protected void createObject(ConnectionHandler connection, DBNConnection conn, DBCredential object) throws SQLException {
        DatabaseAssistantInterface databaseInterface = connection.getAssistantInterface();
        String credentialName = object.getName();
        DBCredentialType credentialType = object.getType();

        if (credentialType == DBCredentialType.PASSWORD) {
            databaseInterface.createPwdCredential(conn,
                    credentialName,
                    object.getAttribute(USER_NAME),
                    object.getAttribute(PASSWORD));

        } else if (credentialType == DBCredentialType.OCI) {
            databaseInterface.createOciCredential(conn,
                    credentialName,
                    object.getAttribute(USER_OCID),
                    object.getAttribute(USER_TENANCY_OCID),
                    object.getAttribute(PRIVATE_KEY),
                    object.getAttribute(FINGERPRINT));
        }
        // update status
        if (object.isEnabled())
            databaseInterface.enableCredential(conn, credentialName);
        else
            databaseInterface.disableCredential(conn, credentialName);
    }

    @Override
    protected void updateObject(ConnectionHandler connection, DBNConnection conn, DBCredential object) throws SQLException {
        DatabaseAssistantInterface databaseInterface = connection.getAssistantInterface();
        Map<DBAttributeType, String> attributes = object.getAttributes();
        String credentialName = object.getName();

        // update attributes
        for (DBAttributeType attribute : attributes.keySet()) {
            String value = attributes.get(attribute);
            if (Strings.isEmpty(value)) continue;
            databaseInterface.updateCredentialAttribute(conn, credentialName, attribute.getId(), value);
        }

        // update status
        if (object.isEnabled())
            databaseInterface.enableCredential(conn, credentialName); else
            databaseInterface.disableCredential(conn, credentialName);
    }

    @Override
    protected void deleteObject(ConnectionHandler connection, DBNConnection conn, DBCredential object) throws SQLException {
        DatabaseAssistantInterface databaseInterface = connection.getAssistantInterface();
        databaseInterface.deleteCredential(conn, object.getName());
    }

    @Override
    protected void enableObject(ConnectionHandler connection, DBNConnection conn, DBCredential object) throws SQLException {
        DatabaseAssistantInterface databaseInterface = connection.getAssistantInterface();
        databaseInterface.enableCredential(conn, object.getName());
    }

    @Override
    protected void disableObject(ConnectionHandler connection, DBNConnection conn, DBCredential object) throws SQLException {
        DatabaseAssistantInterface databaseInterface = connection.getAssistantInterface();
        databaseInterface.disableCredential(conn, object.getName());
    }
}
