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

import com.dbn.common.load.ProgressMonitor;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.common.list.DBObjectListVisitor;

import java.util.List;

import static com.dbn.common.util.Unsafe.cast;

public class DBObjectRecursiveLoaderVisitor implements DBObjectListVisitor{
    public static final DBObjectRecursiveLoaderVisitor INSTANCE = new DBObjectRecursiveLoaderVisitor();

    private DBObjectRecursiveLoaderVisitor() {
    }

    @Override
    public void visit(DBObjectList<?> objectList) {
        if (!objectList.isMaster()) return;

        List<DBObject> objects = cast(objectList.getObjects());
        for (DBObject object : objects) {
            ProgressMonitor.checkCancelled();
            object.visitChildObjects(this, false);
        }
    }
}
