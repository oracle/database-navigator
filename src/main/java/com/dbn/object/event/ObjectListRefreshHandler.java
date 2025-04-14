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

package com.dbn.object.event;

import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.common.Priority;
import com.dbn.common.outcome.Outcome;
import com.dbn.common.outcome.OutcomeHandler;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.lookup.DBObjectRef;

public class ObjectListRefreshHandler implements OutcomeHandler {
    private final DBObjectRef<?> object;

    private ObjectListRefreshHandler(DBObject object) {
        this.object = DBObjectRef.of(object);
    }

    public static ObjectListRefreshHandler create(DBObject object) {
        return new ObjectListRefreshHandler(object);
    }

    @Override
    public void handle(Outcome outcome) {
        DBObject object = this.object.value();
        if (object == null) return;

        BrowserTreeNode parent = object.getParent();
        if (parent instanceof DBObjectList) {
            DBObjectList objectList = (DBObjectList) parent;
            objectList.markDirty();
        }
    }

    @Override
    public Priority getPriority() {
        return Priority.HIGH;
    }
}
