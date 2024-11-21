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

package com.dbn.object.common;

import com.dbn.common.thread.Background;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static com.dbn.common.dispose.Checks.isValid;

public class DBObjectInitializer {
    private final ConnectionRef connection;
    private final Set<String> current = ContainerUtil.newConcurrentSet();

    public DBObjectInitializer(@NotNull ConnectionHandler connection) {
        this.connection = connection.ref();
    }

    public Project getProject() {
        return connection.ensure().getProject();
    }

    public void initObject(DBObjectRef<?> ref) {
        DBObject object = ref.value();
        if (isValid(object)) return;

        DBObjectRef<?> parentRef = ref.getParentRef();
        if (parentRef == null) {
            initRootObject(ref);
        } else  {
            initChildObject(ref, parentRef);
        }
    }

    private void initRootObject(DBObjectRef<?> ref) {
        Project project = getProject();

        String identifier = identifier(ref, null);
        if (current.contains(identifier)) return;

        synchronized (this) {
            if (current.contains(identifier)) return;

            current.add(identifier);
            Background.run(project, () -> {
                try {
                    ref.get();
                } finally {
                    current.remove(identifier);
                }

            });
        }
    }

    private void initChildObject(DBObjectRef<?> ref, DBObjectRef<?> parentRef) {
        Project project = getProject();

        DBObjectType objectType = ref.getObjectType();
        String identifier = identifier(parentRef, objectType);
        if (current.contains(identifier)) return;

        synchronized (this) {
            if (current.contains(identifier)) return;

            current.add(identifier);
            Background.run(project, () -> {
                try {
                    DBObject parent = parentRef.get();
                    if (parent == null) return;

                    parent.getChildObjects(objectType);
                } finally {
                    current.remove(identifier);
                }
            });
        }
    }


    private String identifier(DBObjectRef<?> ref, DBObjectType objectType) {
        return ref.getPath() + "#" + objectType;
    }

}
