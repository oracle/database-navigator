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

import com.dbn.common.util.Actions;
import com.dbn.object.common.DBSchemaObject;
import com.intellij.openapi.actionSystem.DefaultActionGroup;

public class DependenciesActionGroup extends DefaultActionGroup {
    public DependenciesActionGroup(DBSchemaObject object) {
        super("Dependencies", true);
        add(new ObjectDependencyTreeAction(object));
        add(Actions.SEPARATOR);
        add(new ReferencedObjectsListShowAction(object));
        add(new ReferencingObjectsListShowAction(object));
    }
}