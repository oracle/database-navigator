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

package com.dbn.object.impl;

import com.dbn.assistant.provider.AIModel;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.assistant.provider.AIProviderData;
import com.dbn.assistant.provider.AIProviderId;
import com.dbn.browser.ui.HtmlToolTipBuilder;
import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.common.metadata.def.DBProfileMetadata;
import com.dbn.object.DBAIProfile;
import com.dbn.object.DBCredential;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObjectImpl;
import com.dbn.object.common.status.DBObjectStatus;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.dbn.assistant.AssistantType.SELECT_AI;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.Commons.nvln;
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
    private String region;
    private String ociCompartmentId;
    private String ociEndpointId;
    private String ociRuntimeType;
    private String ociApiFormat;
    private AIProvider provider;
    private AIModel model;
    private boolean interactive;
    private double temperature;
    private List<DBObjectRef<?>> objects;

    public DBAIProfileImpl(
            DBSchema parent,
            String name,
            String description,
            String credentialName,
            String region,
            String ociCompartmentId,
            String ociEndpointId,
            String ociRuntimeType,
            String ociApiFormat,
            AIProvider provider,
            AIModel model,
            String objectList,
            double temperature,
            boolean interactive,
            boolean enabled) throws SQLException {
        super(parent, DBProfileMetadata.Record
                .builder()
                .profileName(name)
                .credentialName(credentialName)
                .region(region)
                .ociCompartmentId(ociCompartmentId)
                .ociEndpointId(ociEndpointId)
                .ociRuntimeType(ociRuntimeType)
                .ociApiFormat(ociApiFormat)
                .provider(provider.getApiName())
                .model(model.getApiName())
                .description(description)
                .objectList(objectList)
                .temperature(temperature)
                .enabled(enabled)
                .interactive(interactive)
                .build());
    }

    DBAIProfileImpl(DBSchema parent, DBProfileMetadata metadata) throws SQLException {
        super(parent, metadata);
    }

    @Override
    protected String initObject(ConnectionHandler connection, DBObject parentObject, DBProfileMetadata metadata) throws SQLException {
        String name = metadata.getProfileName();
        credential = new DBObjectRef<>(parentObject.ref(), DBObjectType.CREDENTIAL, metadata.getCredentialName());
        region = metadata.getRegion();
        ociCompartmentId = metadata.getOciCompartmentId();
        ociEndpointId = metadata.getOciEndpointId();
        ociRuntimeType = metadata.getOciRuntimeType();
        ociApiFormat = metadata.getOciApiFormat();
        description = metadata.getDescription();

        String providerApiName = metadata.getProvider();
        String modelApiName = metadata.getModel();

        provider = AIProviderData.getProvider(SELECT_AI, p -> p.getApiName().equals(providerApiName));
        model = provider == null ? null : provider.getModel(m -> m.getApiName().equals(modelApiName));

        interactive = metadata.isInteractive();
        temperature = metadata.getTemperature();
        objects = jsonToObjectList(connection.getConnectionId(), metadata.getObjectList());

        return name;
    }

    @NonNls
    public String getAttributesJson() {
        @NonNls
        Map<String, Object> attributes = new HashMap<>(Map.of(
            "provider", provider.getApiName(),
            "model", model.getApiName(),
            "temperature", temperature,
            "credential_name", nvl(getQuotedCredentialName(), ""),
            "conversation", interactive ? "true" : "false",
            "object_list", convert(objects, o -> objectToAttributes(o))));
        if(region != null) attributes.put("region", region);
        if(ociCompartmentId != null) attributes.put("oci_compartment_id", ociCompartmentId);
        if(ociEndpointId != null) attributes.put("oci_endpoint_id", ociEndpointId);
        if(ociRuntimeType != null) attributes.put("oci_runtimetype", ociRuntimeType);
        if(ociApiFormat != null) attributes.put("oci_apiformat", ociApiFormat);
        return GSON.toJson(attributes);
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
    public AIProviderId getProviderId() {
        return provider == null ? null : provider.getId();
    }

    @Override
    public String getModelId() {
        return model == null ? null : model.getId();
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

    private String getQuotedCredentialName() {
        DBCredential credential = getCredential();
        return credential == null ? null : credential.getName(true);
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

    @Override
    public @Nullable Icon getIcon() {
        boolean disabled = isDisabled();
        DBObjectType objectType = getObjectType();
        Icon icon = disabled  ?
                objectType.getDisabledIcon() :
                interactive ?
                        Icons.DBO_AI_PROFILE_CONVERSATION :
                        Icons.DBO_AI_PROFILE;
        return nvln(icon, objectType.getIcon());
    }
}
