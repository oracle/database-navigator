package com.dbn.object.impl;

import com.dbn.browser.ui.HtmlToolTipBuilder;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.def.DBCredentialMetadata;
import com.dbn.object.DBCredential;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObjectImpl;
import com.dbn.object.common.status.DBObjectStatus;
import com.dbn.object.type.DBAttributeType;
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
    private final Map<DBAttributeType, String> attributes = new HashMap<>();

    public DBCredentialImpl(DBSchema parent, String name, DBCredentialType type, boolean enabled) throws SQLException {
        super(parent, new DBCredentialMetadata.Record(name, type.name(), "", "", enabled));
    }

    DBCredentialImpl(DBSchema parent, DBCredentialMetadata metadata) throws SQLException {
        super(parent, metadata);
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
    public void setAttribute(DBAttributeType attr, String value) {
        attributes.put(attr, value);
    }

    public String getAttribute(DBAttributeType attr) {
        return attributes.get(attr);
    }

    @Override
    public void buildToolTip(HtmlToolTipBuilder ttb) {
        ttb.append(true, getObjectType().getName(), true);
        ttb.createEmptyRow();
        super.buildToolTip(ttb);
    }

    /*********************************************************
     *                     TreeElement                       *
     *********************************************************/

    @Override
    public boolean isLeaf() {
        return true;
    }

}
