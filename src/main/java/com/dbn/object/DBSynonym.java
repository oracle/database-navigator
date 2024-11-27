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

package com.dbn.object;

import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.Nullable;

public interface DBSynonym extends DBSchemaObject {
    @Nullable
    DBObject getUnderlyingObject();

    @Nullable
    DBObjectType getUnderlyingObjectType();

    @Nullable
    static DBObject unwrap(@Nullable DBObject object) {
        if (object == null) return null;

        // TODO check if this still needed (old non-weak-ref based impl)
        //object = object.getUndisposedEntity();

        if (object instanceof DBSynonym) {
            DBSynonym synonym = (DBSynonym) object;
            DBObject underlyingObject = synonym.getUnderlyingObject();
            if (underlyingObject != null) return underlyingObject;
        }
        return object;
    }

}