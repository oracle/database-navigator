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

package com.dbn.execution.compiler.action;

import com.dbn.common.icon.Icons;
import com.dbn.database.DatabaseFeature;
import com.dbn.editor.DBContentType;
import com.dbn.execution.compiler.CompileType;
import com.dbn.object.common.DBSchemaObject;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAware;

import static com.dbn.nls.NlsResources.txt;

public class CompileActionGroup extends DefaultActionGroup implements DumbAware {

    public CompileActionGroup(DBSchemaObject object) {
        super(txt("app.execution.action.Compile"), true);
        boolean debugSupported = DatabaseFeature.DEBUGGING.isSupported(object);
        getTemplatePresentation().setIcon(Icons.OBJECT_COMPILE);
        if (object.getContentType() == DBContentType.CODE_SPEC_AND_BODY) {
            add(new CompileObjectAction(object, DBContentType.CODE_SPEC_AND_BODY, CompileType.NORMAL));
            if (debugSupported) {
                add(new CompileObjectAction(object, DBContentType.CODE_SPEC_AND_BODY, CompileType.DEBUG));
            }
        } else {
            add(new CompileObjectAction(object, DBContentType.CODE, CompileType.NORMAL));
            if (debugSupported) {
                add(new CompileObjectAction(object, DBContentType.CODE, CompileType.DEBUG));
            }
        }

        addSeparator();
        add(new CompileInvalidObjectsAction(object.getSchema()));
    }
}