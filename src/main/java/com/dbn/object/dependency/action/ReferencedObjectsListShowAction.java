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

package com.dbn.object.dependency.action;

import com.dbn.object.action.NavigateToObjectAction;
import com.dbn.object.action.ObjectListShowAction;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.DBSchemaObject;
import com.intellij.openapi.actionSystem.AnAction;

import java.util.List;

import static com.dbn.nls.NlsResources.txt;

public class ReferencedObjectsListShowAction extends ObjectListShowAction {
    public ReferencedObjectsListShowAction(DBSchemaObject object) {
        super(txt("app.objects.action.ReferencedObjects"), object);
    }

    @Override
    public List<DBObject> getObjectList() {
        return ((DBSchemaObject) getSourceObject()).getReferencedObjects();
    }

    @Override
    public String getTitle() {
        return "Objects referenced by " + getSourceObject().getQualifiedNameWithType();
    }

    @Override
    public String getEmptyListMessage() {
        return "No referenced objects found for " + getSourceObject().getQualifiedNameWithType();
    }


    @Override
    public String getListName() {
       return "referenced objects";
   }

    @Override
    protected AnAction createObjectAction(DBObject object) {
        return new NavigateToObjectAction(this.getSourceObject(), object);
    }

}