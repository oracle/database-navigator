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

package com.dbn.object.event;

import com.dbn.common.Priority;
import com.dbn.common.component.ConnectionComponent;
import com.dbn.common.outcome.Outcome;
import com.dbn.common.outcome.OutcomeHandler;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import org.jetbrains.annotations.NotNull;

/**
 * Generic implementation of an {@link OutcomeHandler} that sends out a project notification of type {@link ObjectChangeListener}
 *
 * @author Dan Cioca (Oracle)
 */
public class ObjectChangeNotifier extends ConnectionComponent implements OutcomeHandler {
    private final DBObjectRef<?> object;
    private final ObjectChangeAction action;

    private ObjectChangeNotifier(@NotNull DBObject object, ObjectChangeAction action) {
        super(object.getConnection());
        this.action = action;
        this.object = DBObjectRef.of(object);
    }

    public static OutcomeHandler create(DBObject object, ObjectChangeAction action) {
        return new ObjectChangeNotifier(object, action);
    }

    @Override
    public void handle(Outcome outcome) {
        DBObject object = DBObjectRef.get(this.object);
        if (object == null) return;

        ObjectChangeEvent.notify(action, object);
    }

    @Override
    public Priority getPriority() {
        return Priority.LOW;
    }
}
