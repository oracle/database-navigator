/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.connection.config.parameter.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.util.Map;

/**
 * A dialog that collects Easy Connect URL-specific parameters.  These are distinct from the regular driver
 * "properties" that are set elsewhere.  The dialog is meant be called with any existing set parameters
 * parsed from a syntactically valid Easy Connect URL and will save values back to the same map.
 */
@Getter
public class UrlParameterInputDialog extends DBNDialog<UrlParameterInputForm> {
    private final Map<String, String> parameters;

    public UrlParameterInputDialog(Project project, Map<String, String> parameters) {
        super(project, "Easy-Connect Parameters", false); // TODO specific to EZ_CONNECT - make more generic
        this.parameters = parameters;

        init();
    }

    @NotNull
    @Override
    protected UrlParameterInputForm createForm() {
        return new UrlParameterInputForm(this, parameters);
    }

    @Override
    protected Action[] initializeActions() {
        return actions(
                getOKAction(),
                getCancelAction());
    }

    @Override
    protected void doOKAction() {
        parameters.clear();
        parameters.putAll(getForm().getProperties());
        super.doOKAction();
    }
    
    
}
