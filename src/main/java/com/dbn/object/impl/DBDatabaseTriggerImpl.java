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
import com.dbn.object.DBDatabaseTrigger;
import com.dbn.object.DBSchema;
import com.dbn.object.common.status.DBObjectStatus;
import com.dbn.object.common.status.DBObjectStatusHolder;
import com.dbn.object.type.DBObjectType;
import com.dbn.object.type.DBTriggerEvent;
import com.dbn.object.type.DBTriggerType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.sql.SQLException;

import static com.dbn.common.util.Strings.cachedLowerCase;

class DBDatabaseTriggerImpl extends DBTriggerImpl implements DBDatabaseTrigger {
    DBDatabaseTriggerImpl(DBSchema schema, DBTriggerMetadata metadata) throws SQLException {
        super(schema, metadata);
    }

    @NotNull
    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.DATABASE_TRIGGER;
    }

    @Nullable
    @Override
    public Icon getIcon() {
        DBObjectStatusHolder objectStatus = getStatus();
        boolean valid = objectStatus.is(DBObjectStatus.VALID);
        boolean enabled = objectStatus.is(DBObjectStatus.ENABLED);

        if (valid) {
            boolean debug = objectStatus.is(DBObjectStatus.DEBUG);
            if (enabled) {
                return debug ?
                        Icons.DBO_DATABASE_TRIGGER_DEBUG :
                        Icons.DBO_DATABASE_TRIGGER;
            } else {
                return debug ?
                        Icons.DBO_DATABASE_TRIGGER_DEBUG_DISABLED :
                        Icons.DBO_DATABASE_TRIGGER_DISABLED;
            }
        }

        return enabled ?
                Icons.DBO_DATABASE_TRIGGER_ERR :
                Icons.DBO_DATABASE_TRIGGER_ERR_DISABLED;
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

        triggerDesc.append(" on database");

        ttb.append(false, triggerDesc.toString(), false);

        ttb.createEmptyRow();
        super.buildToolTip(ttb);
    }
}
