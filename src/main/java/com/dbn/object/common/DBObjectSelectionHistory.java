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

import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DBObjectSelectionHistory implements Disposable{
    private final List<DBObjectRef> history = new ArrayList<>();
    private int offset;

    public void add(DBObject object) {
        DBObjectRef objectRef = DBObjectRef.of(object);
        if (objectRef != null) {
            if (history.size() > 0 && history.get(offset).equals(objectRef)) {
                return;
            }
            while (history.size() > offset + 1) {
                history.remove(offset + 1);
            }

            while (history.size() > 30) {
                history.remove(0);
            }
            history.add(objectRef);
            offset = history.size() -1;
        }
    }

    public void clear() {
        history.clear();
    }

    public boolean hasNext() {
        return offset < history.size()-1;
    }

    public boolean hasPrevious() {
        return offset > 0;
    }

    @Nullable
    public DBObject nextNoScroll() {
        return hasNext() ? DBObjectRef.get(history.get(offset + 1)) : null;
    }

    @Nullable
    public DBObject previousNoScroll() {
        return hasPrevious() ? DBObjectRef.get(history.get(offset - 1)) : null;
    }

    @Nullable
    public DBObject next() {
        if (offset < history.size() -1) {
            offset = offset + 1;
            DBObjectRef objectRef = history.get(offset);
            DBObject object = objectRef.get();
            if (object == null) {
                history.remove(objectRef);
                return next();
            }
            return object;
        }
        return null;
    }

    @Nullable
    public DBObject previous() {
        if (offset > 0) {
            offset = offset-1;
            DBObjectRef objectRef = history.get(offset);
            DBObject object = objectRef.get();
            if (object == null) {
                history.remove(objectRef);
                return previous();
            }
            return object;
        }
        return null;
    }

    @Override
    public void dispose() {
        history.clear();
    }
}
