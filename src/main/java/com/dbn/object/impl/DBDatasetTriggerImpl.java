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

import com.dbn.browser.ui.HtmlToolTipBuilder;
import com.dbn.common.icon.Icons;
import com.dbn.database.common.metadata.def.DBTriggerMetadata;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBDataset;
import com.dbn.object.DBDatasetTrigger;
import com.dbn.object.common.status.DBObjectStatus;
import com.dbn.object.common.status.DBObjectStatusHolder;
import com.dbn.object.type.DBObjectType;
import com.dbn.object.type.DBTriggerEvent;
import com.dbn.object.type.DBTriggerType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.SQLException;

import static com.dbn.common.Priority.HIGHEST;
import static com.dbn.common.util.Strings.cachedLowerCase;

class DBDatasetTriggerImpl extends DBTriggerImpl implements DBDatasetTrigger {
    DBDatasetTriggerImpl(DBDataset dataset, DBTriggerMetadata metadata) throws SQLException {
        super(dataset, metadata);
    }

    @Override
    public DBDataset getDataset() {
        return (DBDataset) getParentObject();
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.DATASET_TRIGGER;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        DBObjectStatusHolder objectStatus = getStatus();
        if (objectStatus.is(DBObjectStatus.VALID)) {
            if (objectStatus.is(DBObjectStatus.ENABLED)) {
                if (objectStatus.is(DBObjectStatus.DEBUG)) {
                    return Icons.DBO_TRIGGER_DEBUG;
                } else {
                    return Icons.DBO_TRIGGER;
                }
            } else {
                if (objectStatus.is(DBObjectStatus.DEBUG)) {
                    return Icons.DBO_TRIGGER_DISABLED_DEBUG;
                } else {
                    return Icons.DBO_TRIGGER_DISABLED;
                }
            }
        } else {
            if (objectStatus.is(DBObjectStatus.ENABLED)) {
                return Icons.DBO_TRIGGER_ERR;
            } else {
                return Icons.DBO_TRIGGER_ERR_DISABLED;
            }

        }
    }


    @NotNull
    @Override
    public String getQualifiedName() {
        return getSchemaName() + '.' + getName();
    }

    @Override
    public void buildToolTip(HtmlToolTipBuilder ttb) {
        DBTriggerType triggerType = getTriggerType();
        DBTriggerEvent[] triggeringEvents = getTriggerEvents();
        ttb.append(true, getObjectType().getName(), true);
        StringBuilder triggerDesc = new StringBuilder();
        triggerDesc.append(" - ");
        triggerDesc.append(cachedLowerCase(triggerType.getName()));
        triggerDesc.append(" ") ;

        for (DBTriggerEvent triggeringEvent : triggeringEvents) {
            if (triggeringEvent != triggeringEvents[0]) triggerDesc.append(" or ");
            triggerDesc.append(triggeringEvent.getName());
        }
        triggerDesc.append(" on ");
        triggerDesc.append(getDataset().getName());

        ttb.append(false, triggerDesc.toString(), false);

        ttb.createEmptyRow();
        super.buildToolTip(ttb);
    }

    /*********************************************************
     *                         Loaders                       *
     *********************************************************/

    @Override
    public void executeUpdateDDL(DBContentType contentType, String oldCode, String newCode) throws SQLException {
        DatabaseInterfaceInvoker.execute(HIGHEST,
                "Updating source code",
                "Updating sources of " + getQualifiedNameWithType(),
                getProject(),
                getConnectionId(),
                getSchemaId(),
                conn -> {
                    DatabaseDataDefinitionInterface dataDefinition = getConnection().getDataDefinitionInterface();
                    DBDataset dataset = getDataset();
                    dataDefinition.updateTrigger(
                            dataset.getSchemaName(),
                            dataset.getName(),
                            getName(),
                            oldCode,
                            newCode,
                            conn);
        });
    }
}
