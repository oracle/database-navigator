/*
 * Copyright 2026 Oracle and/or its affiliates
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

import com.dbn.connection.DatabaseEntity;
import com.dbn.object.DBDataset;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.type.DBObjectType;
import com.dbn.object.type.DBTriggerEvent;

import static com.dbn.object.factory.model.DBObjectAttributeType.TRIGGER_TARGET_DATASET;
import static com.dbn.object.type.DBObjectType.DATASET_TRIGGER;
import static com.dbn.object.type.DBTriggerEvent.INSERT;

public class DBDatasetTriggerFactoryAdapter extends DBTriggerFactoryAdapter {
    @Override
    public DBObjectType getObjectType() {
        return DATASET_TRIGGER;
    }

    @Override
    public DBObjectSpec createInput(DatabaseEntity parentEntity) {
        DBObjectSpec input = super.createInput(parentEntity);

        if (parentEntity instanceof DBDataset dataset) {
            input.setAttributeValue(TRIGGER_TARGET_DATASET, dataset.getName());
        }

        return input;
    }

    @Override
    protected DBTriggerEvent[] getDefaultEvents() {
        return new DBTriggerEvent[]{INSERT};
    }
}
