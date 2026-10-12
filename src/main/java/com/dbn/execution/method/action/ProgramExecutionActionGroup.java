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

package com.dbn.execution.method.action;

import com.dbn.common.action.DefaultActionGroup;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBMethod;
import com.dbn.object.DBProgram;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.options.general.WorkspaceFeature;
import com.intellij.openapi.project.Project;

import static com.dbn.nls.NlsResources.txt;
import static com.dbn.options.general.WorkspaceFeature.*;

public class ProgramExecutionActionGroup extends DefaultActionGroup {

    public ProgramExecutionActionGroup(DBSchemaObject object) {
        super(txt("app.execution.action.ExecutePlain"), true);
        Project project = object.getProject();
        if (object.getContentType() == DBContentType.CODE_SPEC_AND_BODY) {
            add(new ProgramMethodExecuteAction((DBProgram) object));
            if (DEBUGGER.isEnabled(project)) {
                add(new ProgramMethodDebugAction((DBProgram) object));
            }
        } else {
            add(new MethodExecuteAction((DBMethod) object, false));
            if (DEBUGGER.isEnabled(project)) {
                add(new MethodDebugAction((DBMethod) object, false));
            }
        }
    }
}
