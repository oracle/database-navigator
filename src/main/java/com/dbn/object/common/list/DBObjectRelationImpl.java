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

package com.dbn.object.common.list;

import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.object.DBCastedObject;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectRelationType;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.util.Commons.nvl;

@Getter
public abstract class DBObjectRelationImpl<S extends DBObject, T extends DBObject> extends StatefulDisposableBase implements DBObjectRelation<S, T> {

    private final DBObjectRelationType relationType;
    private final DBObjectRef<S> sourceObjectRef;
    private final DBObjectRef<T> targetObjectRef;

    private final S sourceObject;
    private final T targetObject;

    public DBObjectRelationImpl(DBObjectRelationType relationType, S sourceObject, T targetObject) {
        this.relationType = relationType;
        assert sourceObject.getObjectType() == relationType.getSourceType();
        assert targetObject.getObjectType() == relationType.getTargetType();
        this.sourceObjectRef = DBObjectRef.of(sourceObject);
        this.targetObjectRef = DBObjectRef.of(targetObject);

        // hold strong reference to objects of type DBCastedObject (no strong references in place)
        this.sourceObject = sourceObject instanceof DBCastedObject ? sourceObject : null;
        this.targetObject = targetObject instanceof DBCastedObject ? targetObject : null;
    }



    public S getSourceObject() {
        return DBObjectRef.get(sourceObjectRef);
    }

    public T getTargetObject() {
        return DBObjectRef.get(targetObjectRef);
    }

    public String toString() {
        String sourceObjectName = sourceObjectRef.getQualifiedNameWithType();
        String targetObjectName = targetObjectRef.getQualifiedNameWithType();
        return nvl(sourceObjectName, "UNKNOWN") + " => " + nvl(targetObjectName, "UNKNOWN");
    }

    /*********************************************************
    *               DynamicContentElement                   *
    *********************************************************/
    @NotNull
    @Override
    public String getName() {
        String sourceObjectName = sourceObjectRef.getQualifiedNameWithType();
        String targetObjectName = targetObjectRef.getQualifiedNameWithType();
        return nvl(sourceObjectName, "UNKNOWN") + "." + nvl(targetObjectName, "UNKNOWN");
    }

    @Override
    public int compareTo(@NotNull Object o) {
        DBObjectRelationImpl remote = (DBObjectRelationImpl) o;
        return sourceObjectRef.compareTo(remote.sourceObjectRef);
    }


    @Override
    public void disposeInner() {
        nullify();
    }
}
