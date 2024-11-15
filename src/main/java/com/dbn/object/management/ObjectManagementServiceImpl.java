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

package com.dbn.object.management;

import com.dbn.DatabaseNavigator;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.outcome.OutcomeHandler;
import com.dbn.common.outcome.OutcomeType;
import com.dbn.object.common.DBObject;
import com.dbn.object.event.ObjectChangeAction;
import com.dbn.object.management.adapter.impl.DBAIProfileManagementAdapter;
import com.dbn.object.management.adapter.impl.DBConstraintManagementAdapter;
import com.dbn.object.management.adapter.impl.DBCredentialManagementAdapter;
import com.dbn.object.management.adapter.impl.DBTriggerManagementAdapter;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static com.dbn.common.exception.Exceptions.unsupported;
import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.object.event.ObjectChangeAction.CREATE;
import static com.dbn.object.event.ObjectChangeAction.DELETE;
import static com.dbn.object.event.ObjectChangeAction.DISABLE;
import static com.dbn.object.event.ObjectChangeAction.ENABLE;
import static com.dbn.object.event.ObjectChangeAction.UPDATE;
import static com.dbn.object.type.DBObjectType.AI_PROFILE;
import static com.dbn.object.type.DBObjectType.CONSTRAINT;
import static com.dbn.object.type.DBObjectType.CREDENTIAL;
import static com.dbn.object.type.DBObjectType.DATABASE_TRIGGER;
import static com.dbn.object.type.DBObjectType.DATASET_TRIGGER;

/**
 * Generic database object management component
 * Exposes CRUD-like actions for the {@link DBObject} entities
 * Internally instantiates the specialized {@link com.dbn.object.management.ObjectManagementAdapter} component,
 * and invokes the MODAL option of the adapter
 *
 * @author Dan Cioca (Oracle)
 */
@Slf4j
@State(
        name = ObjectManagementServiceImpl.COMPONENT_NAME,
        storages = @Storage(DatabaseNavigator.STORAGE_FILE))
final class ObjectManagementServiceImpl extends ProjectComponentBase implements ObjectManagementService, PersistentState {
    public static final String COMPONENT_NAME = "DBNavigator.Project.ObjectManagementService";

    private final Map<DBObjectType, ObjectManagementAdapterFactory> managementAdapters = new HashMap<>();

    public ObjectManagementServiceImpl(@NotNull Project project) {
        super(project, COMPONENT_NAME);
        registerAdapters();
    }

    private void registerAdapters() {
        managementAdapters.put(CONSTRAINT, new DBConstraintManagementAdapter());
        managementAdapters.put(DATASET_TRIGGER, new DBTriggerManagementAdapter());
        managementAdapters.put(DATABASE_TRIGGER, new DBTriggerManagementAdapter());
        managementAdapters.put(CREDENTIAL, new DBCredentialManagementAdapter());
        managementAdapters.put(AI_PROFILE, new DBAIProfileManagementAdapter());
        //...
    }

    @Override
    public boolean supports(DBObject object) {
        return managementAdapters.containsKey(object.getObjectType());
    }

    public void createObject(DBObject object, OutcomeHandler successHandler) {
        invokeModal(object, CREATE, successHandler);
    }

    public void updateObject(DBObject object, OutcomeHandler successHandler) {
        invokeModal(object, UPDATE, successHandler);
    }

    public void deleteObject(DBObject object, OutcomeHandler successHandler) {
        invokeModal(object, DELETE, successHandler);
    }

    public void enableObject(DBObject object, OutcomeHandler successHandler) {
        invokeModal(object, ENABLE, successHandler);
    }

    public void disableObject(DBObject object, OutcomeHandler successHandler) {
        invokeModal(object, DISABLE, successHandler);
    }

    @Override
    public void changeObject(DBObject object, ObjectChangeAction action, OutcomeHandler successHandler) {
        switch (action) {
            case CREATE: createObject(object, successHandler); break;
            case UPDATE: updateObject(object, successHandler); break;
            case DELETE: deleteObject(object, successHandler); break;
            case ENABLE: enableObject(object, successHandler); break;
            case DISABLE: disableObject(object, successHandler); break;
            default: unsupported(action);
        }
    }

    private <T extends DBObject> void  invokeModal(T object, ObjectChangeAction action, OutcomeHandler successHandler) {
        DBObjectType objectType = object.getObjectType();
        ObjectManagementAdapterFactory<T> factory = cast(managementAdapters.get(objectType));
        if (factory == null) throw new UnsupportedOperationException("Not supported for objects of type " + objectType);

        ObjectManagementAdapter<T> adapter = factory.createAdapter(object, action);

        adapter.addOutcomeHandler(OutcomeType.SUCCESS, successHandler);
        adapter.invokeModal();
    }

    /****************************************
     *       PersistentStateComponent       *
     *****************************************/
    @Nullable
    @Override
    public Element getComponentState() {
        return null;
    }

    @Override
    public void loadComponentState(@NotNull Element element) {

    }
}
