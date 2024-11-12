package com.dbn.object.impl;

import com.dbn.assistant.provider.AIModel;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.browser.ui.HtmlToolTipBuilder;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.def.DBProfileMetadata;
import com.dbn.object.DBAIProfile;
import com.dbn.object.DBCredential;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObjectImpl;
import com.dbn.object.common.list.DBObjectNavigationList;
import com.dbn.object.common.status.DBObjectStatus;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Lists.convert;
import static com.dbn.object.common.DBObjectUtil.jsonToObjectList;
import static com.dbn.object.common.DBObjectUtil.objectToAttributes;
import static com.dbn.object.common.property.DBObjectProperty.DISABLEABLE;
import static com.dbn.object.common.property.DBObjectProperty.SCHEMA_OBJECT;

@Getter
public class DBAIProfileImpl extends DBSchemaObjectImpl<DBProfileMetadata> implements DBAIProfile {
    private static final Gson GSON = new GsonBuilder().create();
    private String description;
    private DBObjectRef<DBCredential> credential;
    private AIProvider provider;
    private AIModel model;
    private double temperature;
    private List<DBObjectRef<?>> objects;

    public DBAIProfileImpl(
            DBSchema parent,
            String name,
            String description,
            DBCredential credential,
            AIProvider provider,
            AIModel model,
            String objectList,
            double temperature,
            boolean enabled) throws SQLException {
        super(parent, new DBProfileMetadata.Record(
                name,
                credential.getName(),
                provider.getId(),
                model.getApiName(),
                description,
                objectList,
                temperature,
                enabled));
    }

    DBAIProfileImpl(DBSchema parent, DBProfileMetadata metadata) throws SQLException {
        super(parent, metadata);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBProfileMetadata metadata) throws SQLException {
        String name = metadata.getProfileName();
        credential = new DBObjectRef<>(parentObject.ref(), DBObjectType.CREDENTIAL, metadata.getCredentialName());
        description = metadata.getDescription();
        provider = AIProvider.forId(metadata.getProvider());
        model = AIModel.forApiName(metadata.getModel());
        temperature = metadata.getTemperature();
        objects = jsonToObjectList(connection.getConnectionId(), metadata.getObjectList());

        return name;
    }

    public String getAttributesJson() {
        return GSON.toJson(Map.of(
                "provider", getProvider().getId(),
                "model", getModel().getApiName(),
                "temperature", getTemperature(),
                "credential_name", nvl(getCredentialName(), ""),
                "object_list", convert(objects, o -> objectToAttributes(o))));
    }

    public String getCredentialName() {
        return credential == null ? null : credential.getObjectName();
    }


    @Override
    protected void initProperties() {
        properties.set(SCHEMA_OBJECT, true);
        properties.set(DISABLEABLE, true);
    }

    @Override
    public void initStatus(DBProfileMetadata metadata) throws SQLException {
        boolean enabled = metadata.isEnabled();
        getStatus().set(DBObjectStatus.ENABLED, enabled);
    }


    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.AI_PROFILE;
    }

    @Nullable
    public DBCredential getCredential() {
        return DBObjectRef.get(credential);
    }


    @Override
    public void buildToolTip(HtmlToolTipBuilder ttb) {
        ttb.append(true, getObjectType().getName(), true);
        ttb.createEmptyRow();
        super.buildToolTip(ttb);
    }

    @Override
    public <T extends DBObject> List<T> getChildObjects(DBObjectType objectType) {
        return super.getChildObjects(objectType);
    }

    @Override
    protected @Nullable List<DBObjectNavigationList> createNavigationLists() {
        List<DBObjectNavigationList> navigationLists = new LinkedList<>();
        navigationLists.add(DBObjectNavigationList.create("Profile objects", getObjects()));
        navigationLists.add(DBObjectNavigationList.create("Credential", getCredential()));
        return navigationLists;
    }

    /*********************************************************
     *                     TreeElement                       *
     *********************************************************/

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public List<DBObject> getObjects() {
        return objects.stream().map(o -> o.get()).filter(o -> o != null).collect(Collectors.toList());
    }
}
