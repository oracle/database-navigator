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

package com.dbn.object.diagram.model;

import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;

public final class DBDiagramInput<T extends DBObject> {
    private final DBDiagramDescriptor<T> descriptor;
    private final List<DBObjectRef<T>> roots;
    private final Map<DBObjectRef<T>, List<DBObjectRef<?>>> children;

    public DBDiagramInput(DBDiagramDescriptor<T> descriptor, @NotNull T source) {
        this.descriptor = descriptor;

        Collection<T> rootObjects = this.descriptor.getRootObjects(source);

        List<DBObjectRef<T>> rootRefs = new ArrayList<>(rootObjects.size());
        Map<DBObjectRef<T>, List<DBObjectRef<?>>> childIndex = new LinkedHashMap<>();
        for (T root : rootObjects) {
            DBObjectRef<T> rootRef = DBObjectRef.of(root);
            rootRefs.add(rootRef);
            List<DBObjectRef<?>> childRefs = new ArrayList<>();
            for (DBObject child : descriptor.getChildObjects(root)) {
                childRefs.add(DBObjectRef.of(child));
            }
            childIndex.put(rootRef, unmodifiableList(childRefs));
        }
        this.roots = unmodifiableList(rootRefs);
        this.children = Collections.unmodifiableMap(childIndex);
    }

    @NotNull
    public List<T> getRoots() {
        List<T> result = new ArrayList<>(roots.size());
        for (DBObjectRef<T> rootRef : roots) {
            T root = rootRef.get();
            if (root != null) result.add(root);
        }
        return unmodifiableList(result);
    }

    @NotNull
    public List<DBObject> getChildren(@NotNull T root) {
        List<DBObjectRef<?>> columnRefs = children.get(root.ref());
        if (columnRefs == null) return Collections.emptyList();
        List<DBObject> result = new ArrayList<>(columnRefs.size());
        for (DBObjectRef<?> childRef : columnRefs) {
            DBObject child = childRef.get();
            if (child != null) result.add(child);
        }
        return unmodifiableList(result);
    }
}
