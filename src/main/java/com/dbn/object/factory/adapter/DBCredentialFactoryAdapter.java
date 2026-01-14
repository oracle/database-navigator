/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.object.factory.adapter;

import com.dbn.common.ui.component.DBNComponent;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.database.interfaces.DatabaseAssistantInterface;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.object.DBSchema;
import com.dbn.object.event.ObjectChangeEvent;
import com.dbn.object.factory.ObjectFactoryAdapter;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.factory.ui.DBCredentialFactoryInputForm;
import com.dbn.object.type.DBCredentialType;
import com.dbn.object.type.DBObjectType;

import java.sql.SQLException;
import java.util.List;

import static com.dbn.common.Priority.HIGH;
import static com.dbn.object.event.ObjectChangeAction.CREATE;
import static com.dbn.object.factory.model.DBObjectAttributeType.CREDENTIAL_TYPE;
import static com.dbn.object.factory.model.DBObjectAttributeType.FINGERPRINT;
import static com.dbn.object.factory.model.DBObjectAttributeType.PASSWORD;
import static com.dbn.object.factory.model.DBObjectAttributeType.PRIVATE_KEY;
import static com.dbn.object.factory.model.DBObjectAttributeType.TENANCY_OCID;
import static com.dbn.object.factory.model.DBObjectAttributeType.USER_NAME;
import static com.dbn.object.factory.model.DBObjectAttributeType.USER_OCID;
import static com.dbn.object.type.DBObjectType.CREDENTIAL;

public class DBCredentialFactoryAdapter implements ObjectFactoryAdapter<DBObjectSpec, DBCredentialFactoryInputForm> {

    @Override
    public DBObjectType getObjectType() {
        return CREDENTIAL;
    }

    public DBObjectSpec createInput(DBSchema schema) {
        DBObjectSpec credentialSpec = new DBObjectSpec(schema);
        credentialSpec.setObjectType(CREDENTIAL);
        credentialSpec.setAttributeValue(CREDENTIAL_TYPE, DBCredentialType.PASSWORD);
        return credentialSpec;
    }

    public DBCredentialFactoryInputForm createInputForm(DBNComponent parent, DBObjectSpec input) {
        return new DBCredentialFactoryInputForm(parent, input);
    }

    @Override
    public void validateInput(DBObjectSpec input, List<String> errors) {
    }

    @Override
    public void createObject(DBObjectSpec input) throws SQLException {
        ConnectionId connectionId = input.getConnectionId();
        SchemaId schemaId = input.getSchemaId();

        DatabaseInterfaceInvoker.execute(HIGH,
                "Creating " + input.getObjectType().getTitleCasedName(),
                "Creating " + input.getObjectDescription(),
                input.getProject(),
                connectionId,
                schemaId,
                conn -> {
                    DatabaseAssistantInterface assistantInterface = input.getConnection().getAssistantInterface();
                    DBCredentialType credentialType = input.getAttributeValue(CREDENTIAL_TYPE);

                    if (credentialType == DBCredentialType.OCI) {
                        assistantInterface.createOciCredential(
                                conn,
                                input.getObjectName(true),
                                input.getAttributeValue(USER_OCID),
                                input.getAttributeValue(TENANCY_OCID),
                                input.getAttributeValue(PRIVATE_KEY),
                                input.getAttributeValue(FINGERPRINT));
                    } else if (credentialType == DBCredentialType.PASSWORD) {
                        assistantInterface.createPwdCredential(
                                conn,
                                input.getObjectName(true),
                                input.getAttributeValue(USER_NAME),
                                input.getStringAttributeValue(PASSWORD)
                        );
                    }
                });

        ObjectChangeEvent.notify(CREATE, CREDENTIAL, connectionId, schemaId);
    }
}
