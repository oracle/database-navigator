package com.dbn.object.impl;

import com.dbn.assistant.credential.remote.CredentialManagementService;
import com.dbn.browser.ui.HtmlToolTipBuilder;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.def.DBCredentialMetadata;
import com.dbn.object.DBCredential;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObjectImpl;
import com.dbn.object.common.operation.DBOperationExecutor;
import com.dbn.object.common.status.DBObjectStatus;
import com.dbn.object.type.DBCredentialType;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static com.dbn.object.common.property.DBObjectProperty.DISABLEABLE;
import static com.dbn.object.common.property.DBObjectProperty.SCHEMA_OBJECT;

@Getter
public class DBCredentialImpl extends DBSchemaObjectImpl<DBCredentialMetadata> implements DBCredential {
    private DBCredentialType type;
    private String userName;
    private String comments;
    private final Map<String, String> attributes = new HashMap<>();

    public DBCredentialImpl(DBSchema parent, String name, DBCredentialType type, boolean enabled) throws SQLException {
        super(parent, newCredentialMetadata(name, type, enabled));
    }

    DBCredentialImpl(DBSchema parent, DBCredentialMetadata resultSet) throws SQLException {
        super(parent, resultSet);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBCredentialMetadata metadata) throws SQLException {
        String name = metadata.getCredentialName();
        type = DBCredentialType.valueOf(metadata.getCredentialType());
        userName = metadata.getUserName();
        comments= metadata.getComments();

        return name;
    }

    @Override
    protected void initProperties() {
        properties.set(SCHEMA_OBJECT, true);
        properties.set(DISABLEABLE, true);
    }

    @Override
    public void initStatus(DBCredentialMetadata metadata) throws SQLException {
        boolean enabled = metadata.isEnabled();
        getStatus().set(DBObjectStatus.ENABLED, enabled);
    }


    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.CREDENTIAL;
    }

    @Override
    public void setAttribute(String key, String value) {
        attributes.put(key, value);
    }

    public String getAttribute(String key) {
        return attributes.get(key);
    }

    @Override
    public void buildToolTip(HtmlToolTipBuilder ttb) {
        ttb.append(true, getObjectType().getName(), true);
        ttb.createEmptyRow();
        super.buildToolTip(ttb);
    }

    @Override
    public DBOperationExecutor getOperationExecutor() {
        return operationType -> {
            CredentialManagementService managementService = CredentialManagementService.getInstance(getProject());
            ConnectionHandler connection = getConnection();
            switch (operationType) {
                case ENABLE:  managementService.enableCredential(this, null); break;
                case DISABLE: managementService.disableCredential(this, null); break;
            }
        };
    }


    private static @NotNull DBCredentialMetadata newCredentialMetadata(String name, DBCredentialType type, boolean enabled) {
        return new DBCredentialMetadata() {
            @Override
            public String getCredentialName() throws SQLException {
                return name;
            }

            @Override
            public String getCredentialType() throws SQLException {
                return type.name();
            }

            @Override
            public String getUserName() throws SQLException {
                return "";
            }

            @Override
            public String getComments() throws SQLException {
                return "";
            }

            @Override
            public boolean isEnabled() throws SQLException {
                return enabled;
            }
        };
    }

    /*********************************************************
     *                     TreeElement                       *
     *********************************************************/

    @Override
    public boolean isLeaf() {
        return true;
    }

}
